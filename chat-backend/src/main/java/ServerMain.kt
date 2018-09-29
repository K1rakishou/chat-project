import core.Connection
import core.PacketInfo
import core.packet.Packet
import core.packet.PacketType
import core.packet.TestPacket
import core.security.SecurityUtils
import handler.TestPacketHandler
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.util.ioCoroutineDispatcher
import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.yield
import kotlinx.io.core.IoBuffer
import kotlinx.io.core.readFully
import manager.ConnectionManager
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.net.InetSocketAddress
import java.security.Security


fun main(args: Array<String>) {
  Security.setProperty("crypto.policy", "unlimited")
  Security.addProvider(BouncyCastleProvider())

  Server().run()
}

class Server {
  private val connectionManager = ConnectionManager()
  private val testPacketHandler = TestPacketHandler(connectionManager)

  fun run() {
    runBlocking {
      val server = aSocket(ActorSelectorManager(ioCoroutineDispatcher))
        .tcp()
        .bind(InetSocketAddress("127.0.0.1", 2323))

      println("Started server at ${server.localAddress}")

      while (true) {
        val clientSocket = server.accept()

        launch {
          clientSocket.use { socket ->
            val clientAddress = socket.remoteAddress.toString()
            println("Socket accepted: $clientAddress")

            val readChannel = socket.openReadChannel()
            val connection = Connection(clientAddress, socket.openWriteChannel(autoFlush = false))
            connectionManager.addConnection(clientAddress, connection)

            try {
              listenClient(readChannel, socket, connection)
            } catch (error: Throwable) {
              error.printStackTrace()
            }
          }
        }
      }
    }
  }

  private suspend fun listenClient(readChannel: ByteReadChannel, socket: Socket, connection: Connection) {
    while (!readChannel.isClosedForRead && !readChannel.isClosedForWrite) {
      if (readChannel.availableForRead < Packet.PACKET_HEADER_SIZE) {
        yield()
        continue
      }

      val magicNumber = readChannel.readInt()
      if (magicNumber != Packet.MAGIC_NUMBER) {
        println("Bad magic number: $magicNumber")
        socket.close()
        return
      }

      val bodySize = readChannel.readShort().toInt()

      val packetInfo = IoBuffer.Pool.borrow().use { buffer ->
        readChannel.readFully(buffer, bodySize)

        val packetId = buffer.readLong()
        val version = buffer.readInt()
        val packetType = PacketType.fromShort(buffer.readShort())
        val random1 = buffer.readLong()
        val random2 = buffer.readLong()

        val packetArray = ByteArray(buffer.readRemaining)
        buffer.readFully(packetArray)

        return@use PacketInfo(packetId, version, packetType, TestPacket.fromByteArray(packetArray))
      }

      when (packetInfo.packetType) {
        PacketType.TestPacket -> {
          testPacketHandler.handle(packetInfo.packetId, packetInfo.packetVersion, packetInfo.packet, connection)
        }
      }
    }
  }

}