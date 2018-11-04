package manager

import core.Connection
import core.Constants
import core.Packet
import core.byte_sink.InMemoryByteSink
import core.extensions.forEachChunkAsync
import core.extensions.myWithLock
import core.response.BaseResponse
import core.response.ResponseBuilder
import core.response.UserHasLeftResponsePayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.sync.Mutex
import kotlin.coroutines.CoroutineContext

class ConnectionManager(
  private val chatRoomManager: ChatRoomManager,
  private val responseBuilder: ResponseBuilder
) : CoroutineScope {
  private val job = Job()
  private val connections = mutableMapOf<String, Connection>()
  private val mutex = Mutex()
  private val sendActorChannelCapacity = 2048
  private val sendPacketsActor: SendChannel<Pair<Connection, BaseResponse>>

  override val coroutineContext: CoroutineContext
    get() = job

  init {
    sendPacketsActor = actor(capacity = sendActorChannelCapacity) {
      for (data in channel) {
        val (connection, response) = data

        try {
          if (connection.writeChannel.isClosedForWrite) {
            println("Could not send response because channel with clientId: ${connection.clientId} is closed for writing")
            continue
          }

          connection.writeChannel.let { wc ->
            writeToOutputChannel(wc, responseBuilder.buildResponse(response, InMemoryByteSink.createWithInitialSize()))
            wc.flush()
          }

        } catch (error: Throwable) {
          println("Client's connection was closed while trying to write data into it")
          removeConnection(connection.clientId)
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

  suspend fun sendResponse(clientId: String, response: BaseResponse) {
    val connection = mutex.myWithLock { connections[clientId] }
    if (connection == null) {
      println("Could not send response because connection with clientId: $clientId does not exist")
      return
    }

    sendPacketsActor.send(Pair(connection, response))
  }

  suspend fun addConnection(clientId: String, connection: Connection): Boolean {
    return mutex.myWithLock {
      if (connections.containsKey(clientId)) {
        println("Already contains clientId: $clientId")
        return@myWithLock false
      }

      connections[clientId] = connection
      println("Added connection for client $clientId")

      return@myWithLock true
    }
  }

  suspend fun removeConnection(clientId: String) {
    mutex.myWithLock {
      if (!connections.containsKey(clientId)) {
        println("Does not contain clientId: $clientId")
        return@myWithLock
      }

      try {
        val roomsWithUserNames = chatRoomManager.leaveAllRooms(clientId)

        roomsWithUserNames.forEach { (roomName, userName) ->
          val room = chatRoomManager.getChatRoom(roomName)
          if (room != null) {
            for (userInRoom in room.getEveryone()) {
              sendResponse(userInRoom.user.clientId, UserHasLeftResponsePayload.success(roomName, userName))
            }
          }
        }
      } finally {
        connections[clientId]?.dispose()
        connections.remove(clientId)
      }
    }
    println("Removed connection for client $clientId")
  }
}