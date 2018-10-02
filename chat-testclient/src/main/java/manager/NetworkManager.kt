package manager

import core.Constants
import core.byte_sink.InMemoryByteSink
import extensions.autoRelease
import core.extensions.toHexSeparated
import core.packet.AbstractPacketPayload
import core.packet.Packet
import core.response.BaseResponse
import core.response.CreateRoomResponsePayload
import core.response.GetPageOfPublicRoomsResponsePayload
import core.response.ResponseType
import core.security.SecurityUtils
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.util.ioCoroutineDispatcher
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.ByteWriteChannel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.io.core.IoBuffer
import kotlinx.io.core.readFully
import java.net.InetSocketAddress

class NetworkManager {
  private val ecKeyPair = SecurityUtils.Exchange.generateECKeyPair()
  private val publicKeyEncoded = ecKeyPair.public.encoded
  private lateinit var output: ByteWriteChannel

  val responseQueue = BroadcastChannel<BaseResponse>(128)

  private suspend fun listenServer(readChannel: ByteReadChannel) {
    while (!readChannel.isClosedForRead) {
      val magicNumberFirstByte = readChannel.readByte()
      if (magicNumberFirstByte != Packet.MAGIC_NUMBER_BYTES[0]) {
        println("Bad magicNumber first byte $magicNumberFirstByte")
        continue
      }

      val magicNumberSecondByte = readChannel.readByte()
      if (magicNumberSecondByte != Packet.MAGIC_NUMBER_BYTES[1]) {
        println("Bad magicNumber second byte $magicNumberSecondByte")
        continue
      }

      val magicNumberThirdByte = readChannel.readByte()
      if (magicNumberThirdByte != Packet.MAGIC_NUMBER_BYTES[2]) {
        println("Bad magicNumber third byte $magicNumberThirdByte")
        continue
      }

      val magicNumberFourthByte = readChannel.readByte()
      if (magicNumberFourthByte != Packet.MAGIC_NUMBER_BYTES[3]) {
        println("Bad magicNumber fourth byte $magicNumberFourthByte")
        continue
      }

      val bodySize = readChannel.readInt()

      val packet = IoBuffer.Pool.autoRelease { buffer ->
        readChannel.readFully(buffer, bodySize)

        val id = buffer.readLong()
        val responseType = ResponseType.fromShort(buffer.readShort())

        val packetPayloadRaw = ByteArray(buffer.readRemaining)
        buffer.readFully(packetPayloadRaw)

        println(" >>> RECEIVING: ${packetPayloadRaw.toHexSeparated()}")

        return@autoRelease when (responseType) {
          ResponseType.SendECPublicKeyResponseType -> TODO()
          ResponseType.CreateRoomResponseType -> {
            CreateRoomResponsePayload.fromByteArray(packetPayloadRaw)
          }
          ResponseType.GetPageOfPublicRoomsResponseType -> {
            GetPageOfPublicRoomsResponsePayload.fromByteArray(packetPayloadRaw)
          }
        }
      }

      responseQueue.send(packet)
    }
  }

  private suspend fun writeToOutputChannel(output: ByteWriteChannel, packet: Packet) {
    val sink = InMemoryByteSink.createWithInitialSize(1024)

    output.writeInt(packet.magicNumber)
    output.writeInt(packet.bodySize)
    output.writeLong(packet.packetBody.id)
    output.writeShort(packet.packetBody.type)

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

      output.writeFully(readBuffer, 0, bytesReadCount)

      //for logging
      sink.writeByteArray(readBuffer.copyOfRange(0, bytesReadCount))
      //
    }

    println(" <<< SENDING: ${sink.getArray().toHexSeparated()}")
  }

  suspend fun sendPacket(packet: AbstractPacketPayload) {
    writeToOutputChannel(output, packet.buildPacket(1L))
    output.flush()
  }

  fun run() {
    runBlocking {
      aSocket(ActorSelectorManager(ioCoroutineDispatcher))
        .tcp()
        .connect(InetSocketAddress("127.0.0.1", 2323))
        .apply {
          output = openWriteChannel(autoFlush = false)
          launch { listenServer(openReadChannel()) }
        }
    }
  }
}