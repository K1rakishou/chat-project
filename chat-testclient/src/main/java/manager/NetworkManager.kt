package manager

import core.Constants
import core.Packet
import core.ResponseInfo
import core.byte_sink.InMemoryByteSink
import core.extensions.forEachChunkAsync
import core.extensions.toHex
import core.extensions.toHexSeparated
import core.packet.BasePacket
import core.packet.PacketBuilder
import extensions.readResponseInfo
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.network.util.ioCoroutineDispatcher
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.ByteWriteChannel
import kotlinx.coroutines.experimental.io.close
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.experimental.CoroutineContext

class NetworkManager {
  private val connectionState = AtomicReference<ConnectionState>()
  private val mutex = Mutex()
  private val sendActorChannelCapacity = 128
  private val packetBuilder = PacketBuilder()
  private val reconnectionAttempt = AtomicInteger(0)
  private var cachedHostInfo: HostInfo? = null
  private var reconnectionActor: SendChannel<Unit>? = null
  private var sendPacketsActor: SendChannel<BasePacket>? = null

  val socketEventsQueue = BroadcastChannel<SocketEvent>(128)

  private var socket: Socket? = null
  private var writeChannel: ByteWriteChannel? = null

  private lateinit var readChannel: ByteReadChannel
  private lateinit var byteSinkFileCachePath: String

  init {
    val byteSinkCachePathFile = File(System.getProperty("user.dir") + "\\byte-sink-cache")
    if (!byteSinkCachePathFile.exists()) {
      if (!byteSinkCachePathFile.mkdirs()) {
        throw IllegalStateException("Could not create byteSink cache directory: ${byteSinkCachePathFile.absolutePath}")
      }
    }

    byteSinkFileCachePath = byteSinkCachePathFile.absolutePath
    connectionState.set(ConnectionState.Disconnected)

    reconnectionActor = actor(capacity = 1) {
      for (event in channel) {
        val delayTime = 5000
        println("Trying to reconnect in $delayTime ms...")

        delay(delayTime)
        println("Reconnecting")

        cachedHostInfo?.let { connect(it.host, it.port, true) }
      }
    }
  }

  fun connect(host: String, port: Int, shouldReconnect: Boolean) {
    println("Trying to connect to the server")

    if (!connectionState.compareAndSet(ConnectionState.Disconnected, ConnectionState.Connecting)) {
      println("Already connected or in connecting state")
      return
    }

    launch {
      try {
        mutex.withLock {
          val newSocket = aSocket(ActorSelectorManager(ioCoroutineDispatcher))
            .tcp()
            .connect(InetSocketAddress(host, port))

          if (!connectionState.compareAndSet(ConnectionState.Connecting, ConnectionState.Connected)) {
            println("Connected to server")

            initActors()

            cachedHostInfo = HostInfo(host, port)
            reconnectionAttempt.set(0)

            socket = newSocket
            readChannel = newSocket.openReadChannel()
            writeChannel = newSocket.openWriteChannel(autoFlush = false)

            launch { listenServer(isActive) }
            socketEventsQueue.send(SocketEvent.ConnectedToServer())
          } else {
            println("Connection canceled while trying to connect")

            if (!newSocket.isClosed) {
              newSocket.close()
            }

            disconnect(shouldReconnect)
          }
        }
      } catch (error: Throwable) {
        error.printStackTrace()
        connectionState.set(ConnectionState.Disconnected)

        socketEventsQueue.send(SocketEvent.ErrorWhileConnecting(error))
      }
    }
  }

  fun disconnect(shouldReconnect: Boolean) {
    println("Trying to disconnect from the server")

    if (!connectionState.compareAndSet(ConnectionState.Connecting, ConnectionState.Disconnected) &&
      !connectionState.compareAndSet(ConnectionState.Connected, ConnectionState.Disconnected)) {
      println("Already disconnected")
      return
    }

    launch {
      try {
        mutex.withLock {
          writeChannel?.let { wc ->
            if (!wc.isClosedForWrite) {
              wc.close()
            }
          }

          socket?.let { sckt ->
            if (!sckt.isClosed) {
              sckt.close()
            }
          }

          socketEventsQueue.send(SocketEvent.DisconnectedFromServer())

          sendPacketsActor?.close()
          sendPacketsActor = null

          if (shouldReconnect) {
            if (!reconnectionActor!!.offer(Unit)) {
              println("Already in reconnection state")
            } else {
              println("Disconnected, trying to reconnect")
            }
          }

          println("Disconnected")
        }
      } catch (error: Throwable) {
        error.printStackTrace()
      }
    }
  }

  suspend fun sendPacket(packet: BasePacket) {
    launch {
      writeChannel?.let { wc ->
        if (connectionState.get() != ConnectionState.Connected || wc.isClosedForWrite) {
          disconnect(true)
          return@let
        }

        sendPacketsActor?.send(packet)
      }
    }
  }

  private fun initActors() {
    sendPacketsActor = actor(capacity = sendActorChannelCapacity) {
      for (packet in channel) {
        try {
          val resultPacket = packetBuilder.buildPacket(packet, InMemoryByteSink.createWithInitialSize(1024))
          if (resultPacket == null) {
            println("Could not build packet ${packet::class}")
            continue
          }

          writeToOutputChannel(resultPacket)
        } catch (error: Throwable) {
          println("Disconnected from the server")

          disconnect(true)
        }
      }
    }
  }

  private suspend fun writeToOutputChannel(packet: Packet) {
    writeChannel?.let { wc ->
      wc.writeInt(packet.magicNumber)
      wc.writeInt(packet.bodySize)
      wc.writeShort(packet.type)

      packet.bodyByteSink.getStream().forEachChunkAsync(0, Constants.maxInMemoryByteSinkSize, packet.bodySize) { chunk ->
        wc.writeFully(chunk, 0, chunk.size)
        wc.flush()
      }
    }
  }

  private suspend fun listenServer(isActive: Boolean) {
    println("Listening to the server...")

    try {
      while (isActive && !readChannel.isClosedForRead && connectionState.get() == ConnectionState.Connected) {
        if (!readMagicNumber()) {
          continue
        }

        val bodySize = readChannel.readInt()
        val responseInfo = readChannel.readResponseInfo(byteSinkFileCachePath, bodySize)

        responseInfo.byteSink.getStream().use { stream ->
          //TODO: for debug only! may cause OOM when internal buffer is way too big!
          println(" >>> RECEIVING (${bodySize} bytes): ${stream.readAllBytes().toHexSeparated()}")
        }

        socketEventsQueue.send(SocketEvent.ResponseReceived(responseInfo))
      }
    } catch (error: IOException) {
      println("Remote server dropped the connection")
      socketEventsQueue.send(SocketEvent.DisconnectedFromServer())
    } finally {
      disconnect(true)
    }
  }

  private suspend fun readMagicNumber(): Boolean {
    val magicNumberFirstByte = readChannel.readByte()
    if (magicNumberFirstByte != Packet.MAGIC_NUMBER_BYTES[0]) {
      println("Bad magicNumber first byte ${magicNumberFirstByte.toHex()}")
      return false
    }

    val magicNumberSecondByte = readChannel.readByte()
    if (magicNumberSecondByte != Packet.MAGIC_NUMBER_BYTES[1]) {
      println("Bad magicNumber second byte ${magicNumberSecondByte.toHex()}")
      return false
    }

    val magicNumberThirdByte = readChannel.readByte()
    if (magicNumberThirdByte != Packet.MAGIC_NUMBER_BYTES[2]) {
      println("Bad magicNumber third byte ${magicNumberThirdByte.toHex()}")
      return false
    }

    val magicNumberFourthByte = readChannel.readByte()
    if (magicNumberFourthByte != Packet.MAGIC_NUMBER_BYTES[3]) {
      println("Bad magicNumber fourth byte ${magicNumberFourthByte.toHex()}")
      return false
    }

    return true
  }

  enum class ConnectionState {
    Disconnected,
    Connecting,
    Connected
  }

  sealed class SocketEvent {
    class ConnectedToServer : SocketEvent()
    class ErrorWhileConnecting(val throwable: Throwable) : SocketEvent()
    class DisconnectedFromServer : SocketEvent()
    class ResponseReceived(val responseInfo: ResponseInfo) : SocketEvent()
  }

  data class HostInfo(
    val host: String,
    val port: Int
  )
}