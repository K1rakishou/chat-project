import core.Connection
import core.Packet
import core.PacketType
import core.extensions.readPacketInfo
import core.extensions.toHex
import core.extensions.toHexSeparated
import handler.CreateRoomPacketHandler
import handler.GetPageOfPublicRoomsHandler
import handler.JoinChatRoomPacketHandler
import handler.SendChatMessageHandler
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.util.ioCoroutineDispatcher
import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import manager.ChatRoomManager
import manager.ConnectionManager
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.IOException
import java.net.InetSocketAddress
import java.security.Security


fun main(args: Array<String>) {
  if (args.size != 1) {
    println("Please, provide path to byte-sink-cache directory!")
    return
  }

  Security.addProvider(BouncyCastleProvider())

  Server(args[0]).run()
}

class Server(
  private val byteSinkFileCachePath: String
) {
  private val chatRoomManager = ChatRoomManager()
  private val connectionManager = ConnectionManager(chatRoomManager)

  private val createRoomPacketHandler = CreateRoomPacketHandler(connectionManager, chatRoomManager)
  private val getPageOfPublicChatRoomsHandler = GetPageOfPublicRoomsHandler(connectionManager, chatRoomManager)
  private val joinRoomPacketHandler = JoinChatRoomPacketHandler(connectionManager, chatRoomManager)
  private val sendChatMessageHandler = SendChatMessageHandler(connectionManager, chatRoomManager)

  fun run() {
    runBlocking {
      val server = aSocket(ActorSelectorManager(ioCoroutineDispatcher))
        .tcp()
        .bind(InetSocketAddress("127.0.0.1", 2323))

      //test zone
      chatRoomManager.createChatRoom(true)

//      chatRoomManager.createChatRoom(true).apply {
//        addUser(UserInRoom(User("test_user1", "test_address1", ByteArray(128) { 0xAA.toByte()} )))
//        addUser(UserInRoom(User("test_user2", "test_address2", ByteArray(128) { 0xAB.toByte()} )))
//        addUser(UserInRoom(User("test_user3", "test_address3", ByteArray(128) { 0xAC.toByte()} )))
//
//        addMessage(TextChatMessage(0L, "test_user1", "test message 1"))
//        addMessage(TextChatMessage(1L, "test_user2", "test message 2"))
//        addMessage(TextChatMessage(2L, "test_user3", "test message 3"))
//        addMessage(TextChatMessage(3L, "test_user1", "test message 4"))
//        addMessage(TextChatMessage(4L, "test_user2", "test message 5"))
//        addMessage(TextChatMessage(5L, "test_user3", "test message 6"))
//      }
      //test zone

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
      is IOException -> {
        println("Client: ${clientAddress} forcibly closed the connection")
      }
      else -> error.printStackTrace()
    }
  }

  private suspend fun listenClient(readChannel: ByteReadChannel, clientAddress: String) {
    while (!readChannel.isClosedForRead && !readChannel.isClosedForWrite) {
      if (!readMagicNumber(readChannel)) {
        continue
      }

      val bodySize = readChannel.readInt()
      val packetInfo = readChannel.readPacketInfo(byteSinkFileCachePath, bodySize)

      //TODO: for debug only! may cause OOM when internal buffer is way too big!
      println(" <<< RECEIVING ($bodySize bytes): ${packetInfo.byteSink.getStream().readAllBytes().toHexSeparated()}")

      packetInfo.byteSink.use { byteSink ->
        when (packetInfo.packetType) {
          PacketType.CreateRoomPacketType -> {
            createRoomPacketHandler.handle(packetInfo.packetId, byteSink, clientAddress)
          }
          PacketType.GetPageOfPublicRoomsPacketType -> {
            getPageOfPublicChatRoomsHandler.handle(packetInfo.packetId, byteSink, clientAddress)
          }
          PacketType.JoinRoomPacketType -> {
            joinRoomPacketHandler.handle(packetInfo.packetId, byteSink, clientAddress)
          }
          PacketType.SendChatMessagePacketType -> {
            sendChatMessageHandler.handle(packetInfo.packetId, byteSink, clientAddress)
          }
        }
      }
    }
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