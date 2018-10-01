package core

import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock

data class ChatRoom(
  val roomName: String,
  val roomPasswordHash: String?,
  val isPublic: Boolean,
  val createdOn: Long,
  val userList: MutableList<UserInRoom> = mutableListOf()
) {
  private val mutex = Mutex()

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

  suspend fun getUser(userName: String): UserInRoom? {
    return mutex.withLock {
      return@withLock userList.firstOrNull { it.user.userName == userName }
    }
  }

  suspend fun getEveryoneExcept(userName: String): List<UserInRoom> {
    return mutex.withLock {
      return@withLock mutableListOf<UserInRoom>()
        .apply {
          addAll(userList.filter { it.user.userName == userName })
        }
    }
  }

  suspend fun countUsers(): Int {
    return mutex.withLock {
      return@withLock userList.size
    }
  }

  override fun toString(): String {
    return "[roomName: $roomName, roomPasswordHash: $roomPasswordHash, isPublic: $isPublic, createdOn: $createdOn]"
  }
}