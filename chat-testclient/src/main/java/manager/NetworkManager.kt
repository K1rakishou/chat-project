package manager

import core.Constants
import core.Packet
import core.ResponseInfo
import core.byte_sink.InMemoryByteSink
import core.extensions.toHexSeparated
import core.packet.BasePacket
import extensions.readResponseInfo
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.network.util.ioCoroutineDispatcher
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.ByteWriteChannel
import kotlinx.coroutines.experimental.io.close
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean

class NetworkManager {
//  private val ecKeyPair = SecurityUtils.Exchange.generateECKeyPair()
//  private val publicKeyEncoded = ecKeyPair.public.encoded

  private val isConnected = AtomicBoolean(false)
  private val mutex = Mutex()

  private lateinit var socket: Socket
  private lateinit var writeChannel: ByteWriteChannel
  private lateinit var readChannel: ByteReadChannel

  private lateinit var byteSinkFileCachePath: String

  val socketEventsQueue = BroadcastChannel<SocketEvent>(128)

  init {
    val byteSinkCachePathFile = File(System.getProperty("user.dir") + "\\byte-sink-cache")
    if (!byteSinkCachePathFile.exists()) {
      if (!byteSinkCachePathFile.mkdirs()) {
        throw IllegalStateException("Could not create byteSink cache directory: ${byteSinkCachePathFile.absolutePath}")
      }
    }

    byteSinkFileCachePath = byteSinkCachePathFile.absolutePath
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
            launch { listenServer() }

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

  private suspend fun listenServer() {
    try {
      while (!readChannel.isClosedForRead) {
        if (!readMagicNumber()) {
          continue
        }

        val bodySize = readChannel.readInt()
        val responseInfo = readChannel.readResponseInfo(byteSinkFileCachePath, bodySize)

        //TODO: for debug only! may cause OOM when internal buffer is way too big!
        println(" >>> RECEIVING: ${responseInfo.byteSink.getStream().readAllBytes().toHexSeparated()}")

        socketEventsQueue.send(SocketEvent.ResponseReceived(responseInfo))
      }
    } finally {
      disconnect()
    }
  }

  suspend fun sendPacket(packet: BasePacket) {
    if (!isConnected.get() || writeChannel.isClosedForWrite) {
      disconnect()
      return
    }

    writeToOutputChannel(packet.buildPacket(1L))
    writeChannel.flush()
  }

  private suspend fun writeToOutputChannel(packet: Packet) {
    val sink = InMemoryByteSink.createWithInitialSize(1024)

    writeChannel.writeInt(packet.magicNumber)
    writeChannel.writeInt(packet.bodySize)
    writeChannel.writeLong(packet.packetBody.id)
    writeChannel.writeShort(packet.packetBody.type)

    //for logging
    sink.writeInt(packet.magicNumber)
    sink.writeInt(packet.bodySize)
    sink.writeLong(packet.packetBody.id)
    sink.writeShort(packet.packetBody.type)
    //

    val readBuffer = ByteArray(Constants.MAX_PACKET_SIZE_FOR_MEMORY_HANDLING)

    packet.packetBody.bodyByteSink.getStream().use { bodyStream ->
      val bytesReadCount = bodyStream.read(readBuffer, 0, Constants.MAX_PACKET_SIZE_FOR_MEMORY_HANDLING)
      if (bytesReadCount == -1) {
        return@use
      }

      writeChannel.writeFully(readBuffer, 0, bytesReadCount)

      //for logging
      sink.writeByteArray(readBuffer.copyOfRange(0, bytesReadCount))
      //
    }

    println(" <<< SENDING: ${sink.getArray().toHexSeparated()}")
  }

  private suspend fun readMagicNumber(): Boolean {
    val magicNumberFirstByte = readChannel.readByte()
    if (magicNumberFirstByte != Packet.MAGIC_NUMBER_BYTES[0]) {
      println("Bad magicNumber first byte $magicNumberFirstByte")
      return false
    }

    val magicNumberSecondByte = readChannel.readByte()
    if (magicNumberSecondByte != Packet.MAGIC_NUMBER_BYTES[1]) {
      println("Bad magicNumber second byte $magicNumberSecondByte")
      return false
    }

    val magicNumberThirdByte = readChannel.readByte()
    if (magicNumberThirdByte != Packet.MAGIC_NUMBER_BYTES[2]) {
      println("Bad magicNumber third byte $magicNumberThirdByte")
      return false
    }

    val magicNumberFourthByte = readChannel.readByte()
    if (magicNumberFourthByte != Packet.MAGIC_NUMBER_BYTES[3]) {
      println("Bad magicNumber fourth byte $magicNumberFourthByte")
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