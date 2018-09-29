package core

import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock

data class ChatRoom(
  val roomName: String,
  val roomPasswordHash: String?,
  val isPublic: Boolean
) {
  private val mutex = Mutex()
  private val userList = mutableListOf<UserInRoom>()

  suspend fun addUser(userInRoom: UserInRoom) {
    mutex.withLock { userList.add(userInRoom) }
  }

  suspend fun removeUser(userName: String) {
    mutex.withLock { userList.removeIf { it.user.userName == userName } }
  }

  suspend fun containsUser(userName: String): Boolean {
    return mutex.withLock {
      return@withLock userList
        .firstOrNull { it.user.userName == userName }
        ?.let { true } ?: false
    }
  }
}