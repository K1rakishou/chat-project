package manager

import core.Connection
import core.Constants
import core.byte_sink.InMemoryByteSink
import core.extensions.getMany
import core.extensions.toHexSeparated
import core.packet.Packet
import core.response.BaseResponse
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock

class ConnectionManager {
  private val connections = mutableMapOf<String, Connection>()
  private val mutex = Mutex()
  private val loggingSinkInitialSize = 1024

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
    mutex.withLock { connections.remove(clientAddress) }
    println("Removed connection for client $clientAddress")
  }

  private suspend fun writeToOutputChannel(connection: Connection, packet: Packet) {
    val sink = InMemoryByteSink.createWithInitialSize(loggingSinkInitialSize)

    connection.writeChannel.writeInt(packet.magicNumber)
    connection.writeChannel.writeInt(packet.bodySize)
    connection.writeChannel.writeLong(packet.packetBody.id)
    connection.writeChannel.writeShort(packet.packetBody.type)

    //for logging
    sink.writeInt(packet.magicNumber)
    sink.writeInt(packet.bodySize)
    sink.writeLong(packet.packetBody.id)
    sink.writeShort(packet.packetBody.type)
    //

    val readBuffer = ByteArray(Constants.MAX_PACKET_SIZE_FOR_MEMORY_HANDLING)
    val streamSize = packet.packetBody.bodyByteSink.getWriterPosition()

    packet.packetBody.bodyByteSink.getStream().use { bodyStream ->
      for (offset in 0 until streamSize step Constants.MAX_PACKET_SIZE_FOR_MEMORY_HANDLING) {
        val chunk = if (streamSize - offset > Constants.MAX_PACKET_SIZE_FOR_MEMORY_HANDLING) {
          Constants.MAX_PACKET_SIZE_FOR_MEMORY_HANDLING
        } else {
          streamSize - offset
        }

        val bytesReadCount = bodyStream.read(readBuffer, 0, chunk)
        if (bytesReadCount == -1) {
          break
        }

        connection.writeChannel.writeFully(readBuffer, 0, bytesReadCount)

        //for logging
        sink.writeByteArray(readBuffer.copyOfRange(0, bytesReadCount))
        //
      }

    }

    println(" >>> SENDING BACK: ${sink.getArray().toHexSeparated()}")
  }

  suspend fun send(clientAddress: String, response: BaseResponse) {
    val connection = mutex.withLock { connections.getOrDefault(clientAddress, null) }
    if (connection == null) {
      println("Could not send response because connection with clientAddress: $clientAddress does not exist")
      return
    }

    if (connection.writeChannel.isClosedForWrite) {
      println("Could not send response because channel with clientAddress: $clientAddress is closed for writing")
      return
    }

    writeToOutputChannel(connection, response.buildResponse(0L))
    connection.writeChannel.flush()
  }

  suspend fun broadcast(clientAddressList: List<String>, response: BaseResponse) {
    val connections = mutex.withLock { connections.getMany(clientAddressList) }
    if (connections.isEmpty()) {
      return
    }

    for (connection in connections) {
      if (connection.writeChannel.isClosedForWrite) {
        continue
      }

      writeToOutputChannel(connection, response.buildResponse(0L))
      connection.writeChannel.flush()
    }
  }
}