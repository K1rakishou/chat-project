package manager

import core.Constants
import core.Packet
import core.ResponseInfo
import core.byte_sink.InMemoryByteSink
import core.extensions.toHex
import core.extensions.toHexSeparated
import core.packet.BasePacket
import core.packet.EncryptedPacket
import core.packet.PacketBuilder
import core.packet.UnencryptedPacket
import extensions.readResponseInfo
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.network.util.ioCoroutineDispatcher
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.ByteWriteChannel
import kotlinx.coroutines.experimental.io.close
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import store.MyKeyStore
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean

class NetworkManager {
  private val isConnected = AtomicBoolean(false)
  private val mutex = Mutex()
  private val sendActorChannelCapacity = 128
  private val sendPacketsActor: SendChannel<BasePacket>

  val socketEventsQueue = BroadcastChannel<SocketEvent>(128)

  private val packetBuilder = PacketBuilder()
  private val keyStore = MyKeyStore()

  private lateinit var socket: Socket
  private lateinit var writeChannel: ByteWriteChannel
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

    sendPacketsActor = actor(capacity = sendActorChannelCapacity) {
      for (packet in channel) {
        val resultPacket = when (packet) {
          is UnencryptedPacket -> packetBuilder.buildUnencryptedPacket(packet, InMemoryByteSink.createWithInitialSize(1024))
          is EncryptedPacket -> {
            //TODO: DESTROY PRIVATE KEY
            packetBuilder.buildEncryptedPacket(keyStore.getPrivateKey(), keyStore.getSharedSecret(packet.receiverId), packet, InMemoryByteSink.createWithInitialSize(1024))
          }
          else -> throw IllegalStateException("Not implemented for ${packet::class}")
        }

        if (resultPacket == null) {
          println("Could not build packet ${packet::class}")
          continue
        }

        writeToOutputChannel(resultPacket)
        writeChannel.flush()
      }
    }
  }

  suspend fun sendPacket(packet: BasePacket) {
    if (!isConnected.get() || writeChannel.isClosedForWrite) {
      disconnect()
      return
    }

    sendPacketsActor.send(packet)
  }

  fun isConnected(): Boolean {
    return isConnected.get()
  }

  suspend fun connect() {
    try {
      mutex.withLock {
        if (isConnected.get()) {
          return
        }

        aSocket(ActorSelectorManager(ioCoroutineDispatcher))
          .tcp()
          .connect(InetSocketAddress("127.0.0.1", 2323))
          .let { newSocket ->
            socket = newSocket
            readChannel = newSocket.openReadChannel()
            writeChannel = newSocket.openWriteChannel(autoFlush = false)
            launch { listenServer(isActive) }

            isConnected.set(true)
            socketEventsQueue.send(SocketEvent.ConnectedToServer())
          }
      }
    } catch (error: Throwable) {
      socketEventsQueue.send(SocketEvent.ErrorWhileConnecting(error))
    }
  }

  suspend fun disconnect() {
    mutex.withLock {
      if (!isConnected.get()) {
        return
      }

      if (!writeChannel.isClosedForWrite) {
        writeChannel.close()
      }

      if (!socket.isClosed) {
        socket.close()
      }

      isConnected.set(false)
      socketEventsQueue.send(SocketEvent.DisconnectedFromServer())
    }
  }

  private suspend fun listenServer(isActive: Boolean) {
    try {
      while (isActive && !readChannel.isClosedForRead) {
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
      socketEventsQueue.send(SocketEvent.DisconnectedFromServer())
    } finally {
      disconnect()
    }
  }

  private suspend fun writeToOutputChannel(packet: Packet) {
    val sink = InMemoryByteSink.createWithInitialSize(1024)

    writeChannel.writeInt(packet.magicNumber)
    writeChannel.writeInt(packet.bodySize)
    writeChannel.writeShort(packet.packetBody.type)

    //for logging
    sink.writeInt(packet.magicNumber)
    sink.writeInt(packet.bodySize)
    sink.writeShort(packet.packetBody.type)
    //

    val readBuffer = ByteArray(Constants.maxInMemoryByteSinkSize)

    packet.packetBody.bodyByteSink.getStream().use { bodyStream ->
      val bytesReadCount = bodyStream.read(readBuffer, 0, Constants.maxInMemoryByteSinkSize)
      if (bytesReadCount == -1) {
        return@use
      }

      writeChannel.writeFully(readBuffer, 0, bytesReadCount)

      //for logging
      sink.writeByteArray(readBuffer.copyOfRange(0, bytesReadCount))
      //
    }

    sink.getStream().use { stream ->
      //TODO: for debug only! may cause OOM when internal buffer is way too big!
      println(" <<< SENDING  (${sink.getWriterPosition()} bytes): ${stream.readAllBytes().toHexSeparated()}")
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

  sealed class SocketEvent {
    class ConnectedToServer : SocketEvent()
    class ErrorWhileConnecting(val throwable: Throwable) : SocketEvent()
    class DisconnectedFromServer : SocketEvent()
    class ResponseReceived(val responseInfo: ResponseInfo) : SocketEvent()
  }
}