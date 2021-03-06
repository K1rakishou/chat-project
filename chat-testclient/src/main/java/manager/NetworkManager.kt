package manager

import core.Constants
import core.Packet
import core.ResponseInfo
import core.byte_sink.InMemoryByteSink
import core.extensions.forEachChunkAsync
import core.extensions.myWithLock
import core.extensions.readResponseInfo
import core.extensions.toHex
import core.packet.BasePacket
import core.packet.PacketBuilder
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.close
import kotlinx.coroutines.sync.Mutex
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

class NetworkManager : CoroutineScope {
  private val TAG = NetworkManager::class.simpleName

  @Volatile
  private var connectionState: ConnectionState

  private val job = Job()
  private val shouldReconnectToServer = AtomicBoolean(false)
  private val mutex = Mutex()
  private val sendActorChannelCapacity = 128
  private val packetBuilder = PacketBuilder()
  private val reconnectionAttempt = AtomicInteger(0)
  private var cachedHostInfo: HostInfo? = null
  private var reconnectionActor: SendChannel<Unit>? = null
  private var sendPacketsActor: SendChannel<BasePacket>? = null
  private var socket: Socket? = null
  private var writeChannel: ByteWriteChannel? = null
  private var connectionJob: Job? = null
  private var connectionAttempts: Int = 0

  private lateinit var readChannel: ByteReadChannel
  private var byteSinkFileCachePath: String

  val connectionStateObservable = BehaviorSubject.createDefault<ConnectionState>(ConnectionState.Uninitialized)
  val isConnected: Boolean
    get() = (connectionStateObservable.value == ConnectionState.Connected) || (connectionStateObservable.value == ConnectionState.Reconnected)

  private val responsesQueue = PublishSubject.create<ResponseInfo>().toSerialized()
  val responsesFlowable: Flowable<ResponseInfo>
    get() = responsesQueue.toFlowable(BackpressureStrategy.BUFFER)

  override val coroutineContext: CoroutineContext
    get() = job

  init {
    val byteSinkCachePathFile = File(System.getProperty("user.dir") + "\\byte-sink-cache")
    if (!byteSinkCachePathFile.exists()) {
      if (!byteSinkCachePathFile.mkdirs()) {
        throw IllegalStateException("Could not create byteSink cache directory: ${byteSinkCachePathFile.absolutePath}")
      }
    }

    byteSinkFileCachePath = byteSinkCachePathFile.absolutePath
    connectionState = ConnectionState.Disconnected

    //actor for reconnection
    //it will wait delayTime milliseconds and then will try to connect to the server again
    reconnectionActor = actor(capacity = 1) {
      for (event in channel) {
        val delayTime = 5000L
        println("Trying to reconnect in $delayTime ms...")

        delay(delayTime)
        println("Reconnecting")

        cachedHostInfo?.let { connect(it.ip, it.port) }
      }
    }
  }

  fun shouldReconnectOnDisconnect() {
    shouldReconnectToServer.set(true)
  }

  fun doConnect(ip: String, port: Int) {
    connect(ip, port)
  }

  fun doDisconnect() {
    connectionAttempts = 0

    disconnect()
  }

  private fun connect(ip: String, port: Int) {
    connectionStateObservable.onNext(ConnectionState.Uninitialized)

    connectionJob = launch {
      try {
        mutex.myWithLock {
          println("Trying to connect to the server")

          //if an error has occurred while we were trying to connect to the server the state will be ErrorWhileTryingToConnect
          //so we need to manually reset the state to Disconnected only when it is ErrorWhileTryingToConnect
          if (connectionState is ConnectionState.ErrorWhileTryingToConnect) {
            connectionState = ConnectionState.Disconnected
          }

          //if we are not disconnected - do no try to connect
          if (connectionState !is ConnectionState.Disconnected) {
            println("Already connected or in connecting state")
            return@myWithLock
          }

          connectionState = ConnectionState.Connecting
          connectionStateObservable.onNext(ConnectionState.Connecting)

          delay(TimeUnit.SECONDS.toMillis(1L))

          val newSocket = aSocket(ActorSelectorManager(Dispatchers.IO))
            .tcp()
            .connect(InetSocketAddress(ip, port))

          //check whether the user has canceled the connection
          if (connectionState is ConnectionState.Connecting) {
            println("Connected to server")

            connectionState = ConnectionState.Connected
            initActors()

            cachedHostInfo = HostInfo(ip, port)
            reconnectionAttempt.set(0)

            socket = newSocket
            readChannel = newSocket.openReadChannel()
            writeChannel = newSocket.openWriteChannel(autoFlush = false)

            launch { listenServer(isActive) }

            //send Connected state only on the first connection attempt
            if (connectionAttempts == 0) {
              connectionStateObservable.onNext(ConnectionState.Connected)
            } else {
              connectionStateObservable.onNext(ConnectionState.Reconnected)
            }

            ++connectionAttempts
          } else {
            //if we are not in connecting state that means the connection has been canceled
            println("Connection canceled while trying to connect")

            if (!newSocket.isClosed) {
              newSocket.close()
            }

            disconnect()
          }
        }
      } catch (error: Throwable) {
        error.printStackTrace()
        println("Error while trying to connect to the server: ${error.message ?: "no error message"}")

        connectionState = ConnectionState.Disconnected
        connectionStateObservable.onNext(ConnectionState.ErrorWhileTryingToConnect(error))

        startReconnectingIfCan()
      } finally {
        connectionJob = null
      }
    }
  }

  private fun disconnect() {
    launch {
      try {
        mutex.myWithLock {
          println("Trying to disconnect from the server, shouldReconnect = ${shouldReconnectToServer.get()}")

          //if we are not connected - do nothing
          if (connectionState !is ConnectionState.Connecting && connectionState !is ConnectionState.Connected) {
            println("Already disconnected")
            return@myWithLock
          }

          //cancel any active connection jobs
          connectionJob?.let {
            it.cancel()
            connectionJob = null
          }

          //close channels
          writeChannel?.let { wc ->
            if (!wc.isClosedForWrite) {
              wc.close()
            }
          }

          //sockets
          socket?.let { sckt ->
            if (!sckt.isClosed) {
              try {
                sckt.close()
              } catch (error: Throwable) {
                //do nothing
              }
            }
          }

          //and actors
          sendPacketsActor?.close()
          sendPacketsActor = null

          //set state as disconnected
          connectionState = ConnectionState.Disconnected
          connectionStateObservable.onNext(ConnectionState.Disconnected)

          println("Disconnected")
          startReconnectingIfCan()
        }
      } catch (error: Throwable) {
        error.printStackTrace()
        println("Error while disconnecting: ${error.message ?: "no error message"}")
      }
    }
  }

  private fun startReconnectingIfCan() {
    //if shouldReconnectToServer is true - start reconnection coroutine
    if (shouldReconnectToServer.get()) {
      if (!reconnectionActor!!.offer(Unit)) {
        println("Already trying to reconnect")
      } else {
        println("Disconnected, trying to reconnect")
      }
    }
  }

  fun sendPacket(packet: BasePacket) {
    launch {
      writeChannel?.let { wc ->
        //disconnect if we are not connected or the channel is closed
        if (connectionState !is ConnectionState.Connected || wc.isClosedForWrite) {
          disconnect()
          return@let
        }

        sendPacketsActor?.send(packet)
      }
    }
  }

  private fun initActors() {
    //an actor to send outgoing packets to the server
    sendPacketsActor = actor(capacity = sendActorChannelCapacity) {
      for (packet in channel) {
        try {
          writeChannel?.let { wc ->
            writeToOutputChannel(wc, packetBuilder.buildPacket(packet, InMemoryByteSink.createWithInitialSize(1024)))
            wc.flush()
          }

        } catch (error: Throwable) {
          error.printStackTrace()
          println("Disconnected from the server")

          disconnect()
        }
      }
    }
  }

  private suspend fun writeToOutputChannel(writeChannel: ByteWriteChannel, packet: Packet) {
    writeChannel.writeInt(packet.magicNumber)
    writeChannel.writeInt(packet.bodySize)
    writeChannel.writeShort(packet.type)

    packet.bodyByteSink.getStream().forEachChunkAsync(0, Constants.maxInMemoryByteSinkSize, packet.bodySize) { chunk ->
      writeChannel.writeFully(chunk, 0, chunk.size)
    }
  }

  private suspend fun listenServer(isActive: Boolean) {
    println("Listening to the server...")

    try {
      //while read channel is not closed and state is connected
      while (isActive && !readChannel.isClosedForRead && connectionState is ConnectionState.Connected) {
        //skip everything that's not started with 4 magic bytes
        if (!readMagicNumber()) {
          continue
        }

        //read packet
        val bodySize = readChannel.readInt()
        val responseInfo = readChannel.readResponseInfo(byteSinkFileCachePath, bodySize)

        //send to recipients
        responsesQueue.onNext(responseInfo)
      }
    } catch (error: IOException) {
      println("Remote server dropped the connection")
      connectionStateObservable.onNext(ConnectionState.Disconnected)
    } finally {
      disconnect()
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

  sealed class ConnectionState {
    object Uninitialized : ConnectionState()
    object Disconnected : ConnectionState()
    object Connecting  : ConnectionState()
    class ErrorWhileTryingToConnect(val error: Throwable?) : ConnectionState()
    object Connected : ConnectionState()
    object Reconnected : ConnectionState()
  }

  data class HostInfo(
    val ip: String,
    val port: Int
  )
}