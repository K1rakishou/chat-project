import core.extensions.toHex
import core.extensions.toHexSeparated
import core.packet.AbstractPacketPayload
import core.packet.CreateRoomPacketPayload
import core.packet.Packet
import core.response.BaseResponse
import core.response.CreateRoomResponsePayload
import core.response.ResponseType
import core.security.SecurityUtils
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.util.ioCoroutineDispatcher
import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.writeAvailable
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.yield
import kotlinx.io.core.IoBuffer
import kotlinx.io.core.readFully
import org.bouncycastle.jce.provider.BouncyCastleProvider
import tornadofx.launch
import ui.MyApp
import java.net.InetSocketAddress
import java.security.Security

fun main(args: Array<String>) {
  Security.setProperty("crypto.policy", "unlimited")
  Security.addProvider(BouncyCastleProvider())


  launch<MyApp>(args)
//  Client().run()
}

class Client {
  private val ecKeyPair = SecurityUtils.Exchange.generateECKeyPair()
  private val publicKeyEncoded = ecKeyPair.public.encoded

  private fun packetToBytes(id: Long, packetPayload: AbstractPacketPayload): ByteArray {
    val totalBodySize = (Packet.PACKET_BODY_SIZE + packetPayload.getPayloadSize())
    if (totalBodySize > Int.MAX_VALUE) {
      throw RuntimeException("bodySize exceeds Int.MAX_VALUE: $totalBodySize")
    }

    val packetBody = Packet.PacketBody(
      id,
      packetPayload.getPacketType().value,
      packetPayload.toByteArray().getArray()
    ).toByteBuffer().array()

    return Packet(
      Packet.MAGIC_NUMBER,
      totalBodySize,
      packetBody
    ).toByteArray()
  }

  private suspend fun readResponse(readChannel: ByteReadChannel): BaseResponse? {
    while (!readChannel.isClosedForRead) {
      if (readChannel.availableForRead < Packet.PACKET_HEADER_SIZE) {
        yield()
      }

      val magicNumber = readChannel.readInt()

      if (magicNumber != Packet.MAGIC_NUMBER) {
        println("Bad magic number: $magicNumber")
        return null
      }

      return IoBuffer.Pool.autoRelease { buffer ->
        val bodySize = readChannel.readInt()
        readChannel.readFully(buffer, bodySize)

        val id = buffer.readLong()
        val responseType = ResponseType.fromShort(buffer.readShort())

        val packetPayloadRaw = ByteArray(buffer.readRemaining)
        buffer.readFully(packetPayloadRaw)

        return@autoRelease when (responseType) {
          ResponseType.SendECPublicKeyResponse -> TODO()
          ResponseType.CreateRoomResponse -> {
            CreateRoomResponsePayload.fromByteArray(packetPayloadRaw)
          }
        }
      }
    }

    return null
  }

  fun run() {
    runBlocking {
      val socket = aSocket(ActorSelectorManager(ioCoroutineDispatcher)).tcp().connect(InetSocketAddress("127.0.0.1", 2323))
      val input = socket.openReadChannel()
      val output = socket.openWriteChannel(autoFlush = false)

      val packetBytes = packetToBytes(1L, CreateRoomPacketPayload(true, "test_room", "test_password"))
      println(" <<< SENDING: ${packetBytes.toHexSeparated()}")

      output.writeAvailable(packetBytes)
      output.flush()

      val chatRoomCreatedResponse = readResponse(input) as? CreateRoomResponsePayload
      if (chatRoomCreatedResponse == null) {
        println("Error")
        return@runBlocking
      }

      println("chatRoomCreatedResponse status = ${chatRoomCreatedResponse.status}")
    }
  }
}