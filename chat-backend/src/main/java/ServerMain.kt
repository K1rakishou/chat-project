import core.Connection
import core.PacketInfo
import core.extensions.autoRelease
import core.extensions.toHexSeparated
import core.packet.Packet
import core.packet.PacketType
import handler.CreateRoomPacketHandler
import handler.GetPageOfPublicRoomsHandler
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
  private val getPageOfPublicChatRoomsHandler = GetPageOfPublicRoomsHandler(connectionManager, chatRoomManager)

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
      if (readChannel.availableForRead < Packet.PACKET_MAGIC_NUMBER_SIZE) {
        yield()
        continue
      }

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

      val packetInfo = IoBuffer.Pool.autoRelease { buffer ->
        readChannel.readFully(buffer, bodySize)

        val packetId = buffer.readLong()
        val packetType = PacketType.fromShort(buffer.readShort())

        val packetPayloadRaw = ByteArray(buffer.readRemaining)
        buffer.readFully(packetPayloadRaw)

        return@autoRelease PacketInfo(packetId, packetType, packetPayloadRaw)
      }

      println(" >>> RECEIVING: ${packetInfo.packetPayloadRaw.toHexSeparated()}")

      when (packetInfo.packetType) {
        PacketType.SendECPublicKeyPacketType -> TODO() //SendECPublicKeyPacketType.fromByteArray()
        PacketType.CreateRoomPacketType -> {
          createRoomPacketHandler.handle(packetInfo.packetId, packetInfo.packetPayloadRaw, clientAddress)
        }
        PacketType.GetPageOfPublicRoomsPacketType -> {
          getPageOfPublicChatRoomsHandler.handle(packetInfo.packetId, packetInfo.packetPayloadRaw, clientAddress)
        }
      }
    }
  }

}