package manager

import core.Connection
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
}