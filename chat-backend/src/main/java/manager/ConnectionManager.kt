package manager

import core.Connection
import core.PositionAwareByteArray
import core.extensions.getMany
import core.extensions.toHex
import core.response.BaseResponse
import kotlinx.coroutines.experimental.io.writeFully
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock

class ConnectionManager {
  private val connections = mutableMapOf<String, Connection>()
  private val mutex = Mutex()

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

    val byteArray = PositionAwareByteArray.createWithInitialSize(response.getSize())
    response.toByteArray(byteArray)
    println(" <<< SENDING BACK: ${byteArray.getArray().toHex()}")

    connection.writeChannel.writeFully(byteArray.getArray())
    connection.writeChannel.flush()
  }

  suspend fun sendToMany(clientAddressList: List<String>, response: BaseResponse) {
    val connections = mutex.withLock { connections.getMany(clientAddressList) }
    if (connections.isEmpty()) {
      return
    }

    val byteArray = PositionAwareByteArray.createWithInitialSize(response.getSize())
    response.toByteArray(byteArray)
    println(" <<< SENDING BACK: ${byteArray.getArray().toHex()}")

    for (connection in connections) {
      if (connection.writeChannel.isClosedForWrite) {
        continue
      }

      connection.writeChannel.writeFully(byteArray.getArray())
      connection.writeChannel.flush()
    }
  }
}