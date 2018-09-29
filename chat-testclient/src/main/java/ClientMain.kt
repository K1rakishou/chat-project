import core.packet.IPacketPayload
import core.packet.Packet
import core.packet.SendECPublicKeyPacketPayloadV1
import core.security.SecurityUtils
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.util.ioCoroutineDispatcher
import kotlinx.coroutines.experimental.io.readUTF8Line
import kotlinx.coroutines.experimental.io.writeAvailable
import kotlinx.coroutines.experimental.runBlocking
import java.lang.RuntimeException
import java.net.InetSocketAddress
import java.security.SecureRandom

fun main(args: Array<String>) {
  Client().run()
}

class Client {
  private val ecKeyPair = SecurityUtils.Exchange.generateECKeyPair()
  private val publicKeyEncoded = ecKeyPair.public.encoded

  private fun packetToBytes(id: Long, packetPayload: IPacketPayload): ByteArray {
    val totalBodySize = (Packet.PACKET_BODY_SIZE + packetPayload.getPayloadSize())
    if (totalBodySize > Int.MAX_VALUE) {
      throw RuntimeException("bodySize exceeds Int.MAX_VALUE: $totalBodySize")
    }

    val packetBody = Packet.PacketBody(
      id,
      packetPayload.getPacketVersion(),
      packetPayload.getPacketType(),
      packetPayload.toByteBuffer().array()
    ).toByteBuffer().array()

    return Packet(
      Packet.MAGIC_NUMBER,
      totalBodySize,
      packetBody
    ).toByteArray()
  }

  fun run() {
    runBlocking {
      val socket = aSocket(ActorSelectorManager(ioCoroutineDispatcher)).tcp().connect(InetSocketAddress("127.0.0.1", 2323))
      val input = socket.openReadChannel()
      val output = socket.openWriteChannel(autoFlush = false)

      val packetBytes = packetToBytes(1L, SendECPublicKeyPacketPayloadV1(publicKeyEncoded))

      output.writeAvailable(packetBytes)
      output.flush()

      val response = input.readUTF8Line()

      println("Server said: '$response'")
    }
  }
}