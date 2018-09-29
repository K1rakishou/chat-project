import core.extensions.toHex
import core.packet.AbstractPacketPayload
import core.packet.CreateRoomPacketPayload
import core.packet.Packet
import core.security.SecurityUtils
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.util.ioCoroutineDispatcher
import kotlinx.coroutines.experimental.io.readUTF8Line
import kotlinx.coroutines.experimental.io.writeAvailable
import kotlinx.coroutines.experimental.runBlocking
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.net.InetSocketAddress
import java.security.Security

fun main(args: Array<String>) {
  Security.setProperty("crypto.policy", "unlimited")
  Security.addProvider(BouncyCastleProvider())

  Client().run()
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
      packetPayload.getPacketType(),
      packetPayload.toByteArray().getArray()
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

      val packetBytes = packetToBytes(1L, CreateRoomPacketPayload(true, "test_room", "test_password"))
      println(" <<< SENDING: ${packetBytes.toHex()}")

      output.writeAvailable(packetBytes)
      output.flush()

      val response = input.readUTF8Line()

      println("Server said: '$response'")
    }
  }
}