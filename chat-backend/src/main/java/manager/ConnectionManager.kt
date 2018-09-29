package manager

import core.Connection
import core.getMany
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
      return@withLock true
    }
  }

  suspend fun removeConnection(clientAddress: String) {
    mutex.withLock { connections.remove(clientAddress) }
  }

  suspend fun send(clientAddress: String, data: ByteArray) {
    val connection = mutex.withLock { connections.getOrDefault(clientAddress, null) }
    if (connection == null) {
      return
    }

    connection.writeChannel.writeFully(data)
    connection.writeChannel.flush()
  }

  suspend fun sendToMany(clientAddressList: List<String>, data: ByteArray) {
    val connections = mutex.withLock { connections.getMany(clientAddressList) }
    if (connections.isEmpty()) {
      return
    }

    for (connection in connections) {
      if (connection.writeChannel.isClosedForWrite) {
        continue
      }

      connection.writeChannel.writeFully(data)
      connection.writeChannel.flush()
    }
  }
}