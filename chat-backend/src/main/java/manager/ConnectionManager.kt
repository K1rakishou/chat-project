package manager

import core.Connection
import core.Constants
import core.byte_sink.InMemoryByteSink
import core.extensions.forEachChunkAsync
import core.response.BaseResponse
import core.response.ResponseBuilder
import core.response.UserHasLeftResponsePayload
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlin.coroutines.CoroutineContext

class ConnectionManager(
  private val chatRoomManager: ChatRoomManager,
  private val responseBuilder: ResponseBuilder,
  private val dispatcher: CoroutineDispatcher = newFixedThreadPoolContext(2, "connection-manager")
) : CoroutineScope {
  private val job = Job()
  private val sendActorChannelCapacity = 2048
  private val connectionActor: SendChannel<ActorAction>

  override val coroutineContext: CoroutineContext
    get() = job + dispatcher

  init {
    connectionActor = actor(capacity = sendActorChannelCapacity) {
      val connections = mutableMapOf<String, Connection>()

      consumeEach { action ->
        when (action) {
          is ActorAction.AddConnection -> {
            val isOk = addConnectionInternal(connections, action.clientId, action.connection)
            action.result.complete(isOk)
          }
          is ActorAction.RemoveConnection -> removeConnectionInternal(connections, action.clientId)
          is ActorAction.SendResponse -> sendResponseInternal(connections, action.clientId, action.response)
        }
      }
    }
  }

  suspend fun addConnection(clientId: String, connection: Connection): Boolean {
    val result = CompletableDeferred<Boolean>()
    connectionActor.send(ActorAction.AddConnection(clientId, connection, result))
    return result.await()
  }

  suspend fun removeConnection(clientId: String) {
    connectionActor.send(ActorAction.RemoveConnection(clientId))
  }

  suspend fun sendResponse(clientId: String, response: BaseResponse) {
    connectionActor.send(ActorAction.SendResponse(clientId, response))
  }

  private suspend fun sendResponseInternal(
    connections: MutableMap<String, Connection>,
    clientId: String,
    response: BaseResponse
  ) {
    val connection = connections[clientId]

    try {
      if (connection == null) {
        println("Could not find connection for clientId ($clientId)")
        return
      }

      connection.writeChannel.let { wc ->
        val packet = responseBuilder.buildResponse(response, InMemoryByteSink.createWithInitialSize())

        wc.writeInt(packet.magicNumber)
        wc.writeInt(packet.bodySize)
        wc.writeShort(packet.type)

        val streamSize = packet.bodyByteSink.getWriterPosition()

        packet.bodyByteSink.getStream().forEachChunkAsync(0, Constants.maxInMemoryByteSinkSize, streamSize) { chunk ->
          wc.writeFully(chunk, 0, chunk.size)
        }

        wc.flush()
      }

    } catch (error: Throwable) {
      println("Client's connection was closed while trying to write data into it")
      removeConnectionInternal(connections, clientId)
    }
  }

  private suspend fun addConnectionInternal(
    connections: MutableMap<String, Connection>,
    clientId: String,
    newConnection: Connection
  ): Boolean {
    if (connections.containsKey(clientId)) {
      println("Already contains clientId: $clientId")
      return false
    }

    connections[clientId] = newConnection
    println("Added new connection for client $clientId")

    return true
  }

  private suspend fun removeConnectionInternal(connections: MutableMap<String, Connection>, clientId: String) {
    if (!connections.containsKey(clientId)) {
      println("Does not contain clientId: $clientId")
      return
    }

    try {
      val roomsWithUserNames = chatRoomManager.leaveAllRooms(clientId)

      roomsWithUserNames.forEach { (roomName, userName) ->
        val room = chatRoomManager.getChatRoom(roomName)
        if (room != null) {
          for (userInRoom in room.getEveryone()) {
            sendResponseInternal(connections, userInRoom.user.clientId, UserHasLeftResponsePayload.success(roomName, userName))
          }
        }
      }
    } finally {
      connections[clientId]?.dispose()
      connections.remove(clientId)
    }

    println("Removed connection for client $clientId")
  }

  sealed class ActorAction {
    class AddConnection(val clientId: String,
                        val connection: Connection,
                        val result: CompletableDeferred<Boolean>) : ActorAction()

    class RemoveConnection(val clientId: String) : ActorAction()

    class SendResponse(val clientId: String,
                       val response: BaseResponse) : ActorAction()
  }
}