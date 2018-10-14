package core

import core.collections.RingBuffer
import core.model.drainable.chat_message.BaseChatMessage
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock

data class ChatRoom(
  val chatRoomName: String,
  val roomPasswordHash: String?,
  val isPublic: Boolean,
  val createdOn: Long,
  val userList: MutableSet<UserInRoom> = mutableSetOf(),
  val messageHistory: RingBuffer<BaseChatMessage> = RingBuffer(Constants.maxRoomHistoryMessagesCount)
) {
  private val mutex = Mutex()

  suspend fun addUser(userInRoom: UserInRoom): Boolean {
    return mutex.withLock { userList.add(userInRoom) }
  }

  suspend fun removeUser(userName: String) {
    mutex.withLock { userList.removeIf { it.user.userName == userName } }
  }

  suspend fun addMessage(chatMessage: BaseChatMessage) {
    mutex.withLock { messageHistory.add(chatMessage) }
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

  suspend fun getEveryone(): List<UserInRoom> {
    return mutex.withLock { userList.toMutableList() }
  }

  suspend fun getEveryoneExcept(userName: String): List<UserInRoom> {
    return mutex.withLock {
      return@withLock mutableListOf<UserInRoom>()
        .apply {
          addAll(userList.filter { it.user.userName != userName })
        }
    }
  }

  suspend fun countUsers(): Int {
    return mutex.withLock {
      return@withLock userList.size
    }
  }

  suspend fun hasPassword(): Boolean {
    return mutex.withLock {
      return@withLock roomPasswordHash != null
    }
  }

  suspend fun passwordsMatch(chatRoomPassword: String): Boolean {
    return mutex.withLock {
      return@withLock roomPasswordHash == chatRoomPassword
    }
  }

  suspend fun getMessageHistory(): List<BaseChatMessage> {
    return mutex.withLock {
      return@withLock messageHistory.getAll()
    }
  }

  override fun toString(): String {
    return "[chatRoomName: $chatRoomName, roomPasswordHash: $roomPasswordHash, isPublic: $isPublic, createdOn: $createdOn]"
  }

  fun copy(): ChatRoom {
    return ChatRoom(chatRoomName, roomPasswordHash, isPublic, createdOn, userList.toMutableSet(), messageHistory.clone())
  }
}