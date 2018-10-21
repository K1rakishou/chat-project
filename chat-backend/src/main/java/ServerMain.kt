import core.Connection
import core.Packet
import core.PacketType
import core.extensions.readPacketInfo
import core.extensions.toHex
import core.response.ResponseBuilder
import handler.CreateRoomPacketHandler
import handler.GetPageOfPublicRoomsHandler
import handler.JoinChatRoomPacketHandler
import handler.SendChatMessageHandler
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import manager.ChatRoomManager
import manager.ConnectionManager
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.IOException
import java.net.InetSocketAddress
import java.security.Security


fun main(args: Array<String>) {
  if (args.size != 1) {
    println()
    println("Please, provide a path to a directory where ByteSink cache will be created")
    return
  }

  Security.addProvider(BouncyCastleProvider())

  Server(args[0]).run()
}

class Server(
  private val byteSinkFileCachePath: String
) {
  private val responseBuilder = ResponseBuilder()
  private val chatRoomManager = ChatRoomManager()
  private val connectionManager = ConnectionManager(chatRoomManager, responseBuilder)

  private val createRoomPacketHandler = CreateRoomPacketHandler(connectionManager, chatRoomManager)
  private val getPageOfPublicChatRoomsHandler = GetPageOfPublicRoomsHandler(connectionManager, chatRoomManager)
  private val joinRoomPacketHandler = JoinChatRoomPacketHandler(connectionManager, chatRoomManager)
  private val sendChatMessageHandler = SendChatMessageHandler(connectionManager, chatRoomManager)

  fun run() {
    runBlocking {
      val server = aSocket(ActorSelectorManager(Dispatchers.IO))
        .tcp()
        .bind(InetSocketAddress("0.0.0.0", 2323))

      //test zone
      //test zone

      println("Started server at ${server.localAddress}")

      while (true) {
        val clientSocket = server.accept()

        launch {
          clientSocket.use { socket ->
            val clientAddress = socket.remoteAddress.toString()
            val connection = Connection(clientAddress, socket)

            try {
              if (connectionManager.addConnection(clientAddress, connection)) {
                listenClient(isActive, connection, clientAddress)
              }
            } catch (error: Throwable) {
              printException(error, clientAddress)
            } finally {
              connectionManager.removeConnection(clientAddress)
            }
          }
        }
      }
    }
  }

  private fun printException(error: Throwable, clientAddress: String) {
    //TODO: probably should log it somewhere
    when (error) {
      is ClosedReceiveChannelException -> {
        println("ReceiveChannel has been closed for client ${clientAddress} ")
      }
      is IOException -> {
        println("Client: ${clientAddress} forcibly closed the connection")
      }
      else -> error.printStackTrace()
    }
  }

  private suspend fun listenClient(isActive: Boolean, connection: Connection, clientAddress: String) {
    println("Start listening to the client ${clientAddress}")

    while (isActive && !connection.isDisposed) {
      if (!readMagicNumber(connection.readChannel)) {
        continue
      }

      val bodySize = connection.readChannel.readInt()
      val packetInfo = connection.readChannel.readPacketInfo(byteSinkFileCachePath, bodySize)

      packetInfo.byteSink.use { byteSink ->
        when (packetInfo.packetType) {
          PacketType.CreateRoomPacketType -> {
            createRoomPacketHandler.handle(byteSink, clientAddress)
          }
          PacketType.GetPageOfPublicRoomsPacketType -> {
            getPageOfPublicChatRoomsHandler.handle(byteSink, clientAddress)
          }
          PacketType.JoinRoomPacketType -> {
            joinRoomPacketHandler.handle(byteSink, clientAddress)
          }
          PacketType.SendChatMessagePacketType -> {
            sendChatMessageHandler.handle(byteSink, clientAddress)
          }
        }
      }
    }

    println("Stop listening to the client ${clientAddress}")
  }

  private suspend fun readMagicNumber(readChannel: ByteReadChannel): Boolean {
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

}