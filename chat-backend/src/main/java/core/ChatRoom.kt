package core

import core.collections.RingBuffer
import core.extensions.myWithLock
import core.model.drainable.chat_message.BaseChatMessage
import kotlinx.coroutines.experimental.sync.Mutex

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
    return mutex.myWithLock { userList.add(userInRoom) }
  }

  suspend fun removeUser(userName: String) {
    mutex.myWithLock { userList.removeIf { it.user.userName == userName } }
  }

  suspend fun addMessage(chatMessage: BaseChatMessage) {
    mutex.myWithLock { messageHistory.add(chatMessage) }
  }

  suspend fun containsUser(userName: String): Boolean {
    return mutex.myWithLock {
      return@myWithLock userList
        .firstOrNull { it.user.userName == userName }
        ?.let { true } ?: false
    }
  }

  suspend fun getUser(userName: String): UserInRoom? {
    return mutex.myWithLock {
      return@myWithLock userList.firstOrNull { it.user.userName == userName }
    }
  }

  suspend fun getEveryone(): List<UserInRoom> {
    return mutex.myWithLock { userList.toMutableList() }
  }

  suspend fun getEveryoneExcept(userName: String): List<UserInRoom> {
    return mutex.myWithLock {
      return@myWithLock mutableListOf<UserInRoom>()
        .apply {
          addAll(userList.filter { it.user.userName != userName })
        }
    }
  }

  suspend fun countUsers(): Int {
    return mutex.myWithLock {
      return@myWithLock userList.size
    }
  }

  suspend fun hasPassword(): Boolean {
    return mutex.myWithLock {
      return@myWithLock roomPasswordHash != null
    }
  }

  suspend fun passwordsMatch(chatRoomPassword: String): Boolean {
    return mutex.myWithLock {
      return@myWithLock roomPasswordHash == chatRoomPassword
    }
  }

  suspend fun getMessageHistory(): List<BaseChatMessage> {
    return mutex.myWithLock {
      return@myWithLock messageHistory.getAll()
    }
  }

  override fun toString(): String {
    return "[chatRoomName: $chatRoomName, roomPasswordHash: $roomPasswordHash, isPublic: $isPublic, createdOn: $createdOn]"
  }

  fun copy(): ChatRoom {
    return ChatRoom(chatRoomName, roomPasswordHash, isPublic, createdOn, userList.toMutableSet(), messageHistory.clone())
  }
}