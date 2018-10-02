import core.*
import core.byte_sink.InMemoryByteSink
import core.byte_sink.OnDiskByteSink
import core.extensions.autoRelease
import core.packet.Packet
import core.packet.PacketType
import core.utils.TimeUtils
import handler.CreateRoomPacketHandler
import handler.GetPageOfPublicRoomsHandler
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.util.ioCoroutineDispatcher
import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.readAvailable
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.io.core.IoBuffer
import kotlinx.io.core.readFully
import manager.ChatRoomManager
import manager.ConnectionManager
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.io.RandomAccessFile
import java.net.InetSocketAddress
import java.security.Security


fun main(args: Array<String>) {
  if (args.size != 1) {
    println("Please, provide path to byte-sink-cache directory!")
    return
  }

  Security.setProperty("crypto.policy", "unlimited")
  Security.addProvider(BouncyCastleProvider())

  Server(args[0]).run()
}

class Server(
  private val byteSinkFileCachePath: String
) {
  private val connectionManager = ConnectionManager()
  private val chatRoomManager = ChatRoomManager()

  private val createRoomPacketHandler = CreateRoomPacketHandler(connectionManager, chatRoomManager)
  private val getPageOfPublicChatRoomsHandler = GetPageOfPublicRoomsHandler(connectionManager, chatRoomManager)

  fun run() {
    runBlocking {
      val server = aSocket(ActorSelectorManager(ioCoroutineDispatcher))
        .tcp()
        .bind(InetSocketAddress("127.0.0.1", 2323))

      //test zone
      chatRoomManager.createChatRoom(true).apply {
        addUser(UserInRoom(User("1", ByteArray(1))))
        addUser(UserInRoom(User("2", ByteArray(1))))
        addUser(UserInRoom(User("3", ByteArray(1))))
        addUser(UserInRoom(User("4", ByteArray(1))))
        addUser(UserInRoom(User("5", ByteArray(1))))
      }
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
      if (!readMagicNumber(readChannel)) {
        continue
      }

      val bodySize = readChannel.readInt()
      val packetInfo = readPacketInfo(bodySize, readChannel)

      packetInfo.byteSink.use { byteSink ->
        when (packetInfo.packetType) {
          PacketType.SendECPublicKeyPacketType -> TODO() //SendECPublicKeyPacketType.fromByteSink()
          PacketType.CreateRoomPacketType -> {
            createRoomPacketHandler.handle(packetInfo.packetId, byteSink, clientAddress)
          }
          PacketType.GetPageOfPublicRoomsPacketType -> {
            getPageOfPublicChatRoomsHandler.handle(packetInfo.packetId, byteSink, clientAddress)
          }
        }
      }
    }
  }

  private suspend fun readPacketInfo(bodySize: Int, readChannel: ByteReadChannel): PacketInfo {
    var packetInfo: PacketInfo? = null

    if (bodySize <= Constants.MAX_PACKET_SIZE_FOR_MEMORY_HANDLING) {
      IoBuffer.Pool.autoRelease { buffer ->
        readChannel.readFully(buffer, bodySize)

        val packetId = buffer.readLong()
        val packetType = PacketType.fromShort(buffer.readShort())

        val packetPayloadRaw = ByteArray(buffer.readRemaining)
        buffer.readFully(packetPayloadRaw)

        packetInfo = PacketInfo(packetId, packetType, InMemoryByteSink.fromArray(packetPayloadRaw))
      }
    } else {
      val file = File("$byteSinkFileCachePath\\test_file-${TimeUtils.getCurrentTime()}.tmp")
      val randomAccessFile = RandomAccessFile(file, "w")

      randomAccessFile.use { raf ->
        for (offset in 0 until bodySize step Constants.MAX_PACKET_SIZE_FOR_MEMORY_HANDLING) {
          val chunk = if (bodySize - offset > Constants.MAX_PACKET_SIZE_FOR_MEMORY_HANDLING) {
            Constants.MAX_PACKET_SIZE_FOR_MEMORY_HANDLING
          } else {
            bodySize - offset
          }

          val array = ByteArray(chunk)
          readChannel.readAvailable(array)
          raf.write(array)

          val sink = OnDiskByteSink.fromFile(file)

          val packetId = sink.readLong()
          val packetType = PacketType.fromShort(sink.readShort())

          packetInfo = PacketInfo(packetId, packetType, sink)
        }
      }
    }

    return packetInfo!!
  }

  private suspend fun readMagicNumber(readChannel: ByteReadChannel): Boolean {
    val magicNumberFirstByte = readChannel.readByte()
    if (magicNumberFirstByte != Packet.MAGIC_NUMBER_BYTES[0]) {
      println("Bad magicNumber first byte $magicNumberFirstByte")
      return false
    }

    val magicNumberSecondByte = readChannel.readByte()
    if (magicNumberSecondByte != Packet.MAGIC_NUMBER_BYTES[1]) {
      println("Bad magicNumber second byte $magicNumberSecondByte")
      return false
    }

    val magicNumberThirdByte = readChannel.readByte()
    if (magicNumberThirdByte != Packet.MAGIC_NUMBER_BYTES[2]) {
      println("Bad magicNumber third byte $magicNumberThirdByte")
      return false
    }

    val magicNumberFourthByte = readChannel.readByte()
    if (magicNumberFourthByte != Packet.MAGIC_NUMBER_BYTES[3]) {
      println("Bad magicNumber fourth byte $magicNumberFourthByte")
      return false
    }

    return true
  }

}