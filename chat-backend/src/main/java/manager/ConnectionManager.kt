package manager

import core.Connection
import core.Constants
import core.Packet
import core.byte_sink.InMemoryByteSink
import core.extensions.forEachChunkAsync
import core.extensions.tryWithLock
import core.response.BaseResponse
import core.response.ResponseBuilder
import core.response.UserHasLeftResponsePayload
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.io.ByteWriteChannel
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock

class ConnectionManager(
  private val chatRoomManager: ChatRoomManager,
  private val responseBuilder: ResponseBuilder
) {
  private val connections = mutableMapOf<String, Connection>()
  private val mutex = Mutex()
  private val sendActorChannelCapacity = 2048
  private val sendPacketsActor: SendChannel<Pair<Connection, BaseResponse>>

  init {
    sendPacketsActor = actor(capacity = sendActorChannelCapacity) {
      for (data in channel) {
        val (connection, response) = data

        try {
          if (connection.writeChannel.isClosedForWrite) {
            println("Could not send response because channel with clientAddress: ${connection.clientAddress} is closed for writing")
            continue
          }

          connection.writeChannel.let { wc ->
            writeToOutputChannel(wc, responseBuilder.buildResponse(response, InMemoryByteSink.createWithInitialSize()))
            wc.flush()
          }

        } catch (error: Throwable) {
          println("Client's connection was closed while trying to write data into it")
          removeConnection(connection.clientAddress)
        }
      }
    }
  }

  private suspend fun writeToOutputChannel(writeChannel: ByteWriteChannel, packet: Packet) {
    writeChannel.writeInt(packet.magicNumber)
    writeChannel.writeInt(packet.bodySize)
    writeChannel.writeShort(packet.type)

    val streamSize = packet.bodyByteSink.getWriterPosition()

    packet.bodyByteSink.getStream().forEachChunkAsync(0, Constants.maxInMemoryByteSinkSize, streamSize) { chunk ->
      writeChannel.writeFully(chunk, 0, chunk.size)
    }
  }

  suspend fun sendResponse(clientAddress: String, response: BaseResponse) {
    val connection = mutex.tryWithLock { connections.getOrDefault(clientAddress, null) }
    if (connection == null) {
      println("Could not send response because connection with clientAddress: $clientAddress does not exist")
      return
    }

    sendPacketsActor.send(Pair(connection, response))
  }

  suspend fun addConnection(clientAddress: String, connection: Connection): Boolean {
    return mutex.withLock {
      if (connections.containsKey(clientAddress)) {
        println("Already contains clientAddress: $clientAddress")
        return@withLock false
      }

      connections[clientAddress] = connection
      println("Added connection for client $clientAddress")

      return@withLock true
    }
  }

  suspend fun removeConnection(clientAddress: String) {
    println("Removing connection for client ${clientAddress}")

    mutex.withLock {
      if (!connections.containsKey(clientAddress)) {
        println("Does not contain clientAddress: $clientAddress")
        return@withLock
      }

      try {
        val roomsWithUserNames = chatRoomManager.leaveAllRooms(clientAddress)
        println("roomsWithUserNames = $roomsWithUserNames")

        roomsWithUserNames.forEach { (roomName, userName) ->
          val room = chatRoomManager.getChatRoom(roomName)
          println("room = $room")

          if (room != null) {
            for (userInRoom in room.getEveryone()) {
              println("userInRoom = $userInRoom, roomName = $roomName, userName = $userName")

              sendResponse(userInRoom.user.clientAddress, UserHasLeftResponsePayload.success(roomName, userName))
            }
          }
        }
      } finally {
        connections[clientAddress]?.dispose()
        connections.remove(clientAddress)
      }
    }
    println("Removed connection for client $clientAddress")
  }
}