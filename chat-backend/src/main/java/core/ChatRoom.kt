package core

import core.collections.RingBuffer
import core.model.drainable.chat_message.BaseChatMessage
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
  private val messagesHistoryMaxSize = 100
  private val messageHistory: RingBuffer<BaseChatMessage>

  init {
    messageHistory = RingBuffer(messagesHistoryMaxSize)
  }

  suspend fun addUser(userInRoom: UserInRoom) {
    mutex.withLock { userList.add(userInRoom) }
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
    return "[roomName: $roomName, roomPasswordHash: $roomPasswordHash, isPublic: $isPublic, createdOn: $createdOn]"
  }

  fun copy(): ChatRoom {
    return ChatRoom(roomName, roomPasswordHash, isPublic, createdOn, userList.toMutableList())
  }
}