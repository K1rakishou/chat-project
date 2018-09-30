import core.Connection
import core.PacketInfo
import core.extensions.autoRelease
import core.extensions.toHexSeparated
import core.packet.Packet
import core.packet.PacketType
import handler.CreateRoomPacketHandler
import io.ktor.network.selector.ActorSelectorManager
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
import manager.ChatRoomManager
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
  private val chatRoomManager = ChatRoomManager()

  private val createRoomPacketHandler = CreateRoomPacketHandler(connectionManager, chatRoomManager)

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
            val readChannel = socket.openReadChannel()

            try {
              connectionManager.addConnection(clientAddress, Connection(clientAddress, socket.openWriteChannel(autoFlush = false)))
              listenClient(readChannel, clientAddress)
            } catch (error: Throwable) {
              error.printStackTrace()
            } finally {
              connectionManager.removeConnection(clientAddress)
            }
          }
        }
      }
    }
  }

  private suspend fun listenClient(readChannel: ByteReadChannel, clientAddress: String) {
    while (!readChannel.isClosedForRead && !readChannel.isClosedForWrite) {
      if (readChannel.availableForRead < Packet.PACKET_HEADER_SIZE) {
        yield()
        continue
      }

      val magicNumber = readChannel.readInt()
      if (magicNumber != Packet.MAGIC_NUMBER) {
        println("Bad magic number: $magicNumber")
        return
      }

      val packetInfo = IoBuffer.Pool.autoRelease { buffer ->
        val bodySize = readChannel.readInt()
        readChannel.readFully(buffer, bodySize)

        val packetId = buffer.readLong()
        val packetType = PacketType.fromShort(buffer.readShort())

        val packetPayloadRaw = ByteArray(buffer.readRemaining)
        buffer.readFully(packetPayloadRaw)

        return@autoRelease PacketInfo(packetId, packetType, packetPayloadRaw)
      }

      println(" >>> RECEIVING: ${packetInfo.packetPayloadRaw.toHexSeparated()}")

      when (packetInfo.packetType) {
        PacketType.SendECPublicKeyPacketPayload -> TODO() //SendECPublicKeyPacketPayload.fromByteArray()
        PacketType.CreateRoomPacketPayload -> {
          createRoomPacketHandler.handle(packetInfo.packetId, packetInfo.packetPayloadRaw, clientAddress)
        }
      }
    }
  }

}