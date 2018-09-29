import core.packet.Packet
import core.packet.PacketType
import core.packet.TestPacket
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

  val random = SecureRandom()

  fun packetToBytes(id: Long, data: ByteArray): ByteArray {
    val totalBodySize = (Packet.PACKET_BODY_SIZE + data.size).toShort()

    if (totalBodySize > Short.MAX_VALUE) {
      throw RuntimeException("bodySize exceeds Short.MAX_VALUE: $totalBodySize")
    }

    val packetBody = Packet.PacketBody(
      id,
      TestPacket.PACKET_VERSION,
      PacketType.TestPacket,
      random.nextLong(),
      random.nextLong(),
      data
    )

    val packet = Packet(
      Packet.MAGIC_NUMBER,
      totalBodySize,
      packetBody
    )

    return packet.toByteArray()
  }

  runBlocking {
    val socket = aSocket(ActorSelectorManager(ioCoroutineDispatcher)).tcp().connect(InetSocketAddress("127.0.0.1", 2323))
    val input = socket.openReadChannel()
    val output = socket.openWriteChannel(autoFlush = false)

    val packetBytes = packetToBytes(1L, TestPacket(System.currentTimeMillis(), "This is a test message").toByteBuffer().array())

    output.writeAvailable(packetBytes)
    output.flush()

    val response = input.readUTF8Line()

    println("Server said: '$response'")
  }
}