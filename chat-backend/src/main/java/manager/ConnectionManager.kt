package manager

import core.Connection
import core.Constants
import core.Packet
import core.byte_sink.InMemoryByteSink
import core.extensions.forEachChunkAsync
import core.extensions.toHexSeparated
import core.response.BaseResponse
import core.response.ResponseBuilder
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.io.close
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock

class ConnectionManager(
  private val chatRoomManager: ChatRoomManager,
  private val responseBuilder: ResponseBuilder
) {
  private val connections = mutableMapOf<String, Connection>()
  private val mutex = Mutex()
  private val loggingSinkInitialSize = 1024
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

          //TODO: probably should remove the connection from the connection map and also send to every room this user joined that user has disconnected
          writeToOutputChannel(connection, responseBuilder.buildResponse(response, InMemoryByteSink.createWithInitialSize()))
          connection.writeChannel.flush()

        } catch (error: Throwable) {
          println("Client's connection was closed while trying to write data to it")

          removeConnection(connection.clientAddress)
        }
      }
    }
  }

  suspend fun sendResponse(clientAddress: String, response: BaseResponse) {
    val connection = mutex.withLock { connections.getOrDefault(clientAddress, null) }
    if (connection == null) {
      println("Could not send response because connection with clientAddress: $clientAddress does not exist")
      return
    }

    sendPacketsActor.send(Pair(connection, response))
  }

  suspend fun addConnection(clientAddress: String, connection: Connection): Boolean {
    return mutex.withLock {
      if (connections.containsKey(clientAddress)) {
        return@withLock false
      }

      connections[clientAddress] = connection
      println("Added connection for client $clientAddress")

      return@withLock true
    }
  }

  suspend fun removeConnection(clientAddress: String) {
    mutex.withLock {
      try {
        connections[clientAddress]?.let {
          if (!it.writeChannel.isClosedForWrite) {
            it.writeChannel.close()
          }
        }
      } finally {
        connections.remove(clientAddress)
      }
    }
    println("Removed connection for client $clientAddress")
  }

  private suspend fun writeToOutputChannel(connection: Connection, packet: Packet) {
    val loggingSink = InMemoryByteSink.createWithInitialSize(loggingSinkInitialSize)

    connection.writeChannel.writeInt(packet.magicNumber)
    connection.writeChannel.writeInt(packet.bodySize)
    connection.writeChannel.writeShort(packet.type)

    //for logging
    loggingSink.writeInt(packet.magicNumber)
    loggingSink.writeInt(packet.bodySize)
    loggingSink.writeShort(packet.type)
    //

    val streamSize = packet.bodyByteSink.getWriterPosition()

    packet.bodyByteSink.getStream().forEachChunkAsync(0, Constants.maxInMemoryByteSinkSize, streamSize) { chunk ->
      connection.writeChannel.writeFully(chunk, 0, chunk.size)

      //for logging
      loggingSink.writeByteArray(chunk)
      //
    }

    loggingSink.getStream().use { stream ->
      //TODO: for debug only! may cause OOM when internal buffer is way too big!
      println(" >>> SENDING (${loggingSink.getWriterPosition()} bytes): ${stream.readAllBytes().toHexSeparated()}")
    }
  }
}