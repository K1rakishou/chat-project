package core

import core.collections.RingBuffer
import core.extensions.myWithLock
import core.model.drainable.chat_message.BaseChatMessage
import kotlinx.coroutines.experimental.sync.Mutex
import repository.mapper.BaseChatMessageMapper
import repository.model.BaseChatMessageData
import java.lang.IllegalStateException
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

data class ChatRoom(
  val chatRoomName: String,
  val chatRoomImageUrl: String,
  val roomPasswordHash: ByteArray?,
  val isPublic: Boolean,
  val createdOn: Long,
  val userList: MutableSet<UserInRoom> = mutableSetOf(),
  val messageHistory: RingBuffer<BaseChatMessageData> = RingBuffer(Constants.maxRoomHistoryMessagesCount)
) {
  private val mutex = Mutex()
  private val messageIdCounter = AtomicInteger(0)

  suspend fun addUser(userInRoom: UserInRoom): Boolean {
    return mutex.myWithLock { userList.add(userInRoom) }
  }

  suspend fun removeUser(userName: String) {
    mutex.myWithLock { userList.removeIf { it.user.userName == userName } }
  }

  suspend fun addMessage(chatMessage: BaseChatMessage): Int {
    if (chatMessage.serverMessageId != -1) {
      throw IllegalStateException("serverMessageId should be initialized here!")
    }

    val serverMessageId = messageIdCounter.getAndIncrement()
    val chatMessageData = BaseChatMessageMapper.FromBaseChatMessage.toBaseChatMessageData(serverMessageId, chatMessage)

    mutex.myWithLock { messageHistory.add(chatMessageData) }
    return serverMessageId
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

  suspend fun passwordsMatch(chatRoomPassword: ByteArray): Boolean {
    return mutex.myWithLock {
      return@myWithLock Arrays.equals(roomPasswordHash, chatRoomPassword)
    }
  }

  suspend fun getMessageHistory(): List<BaseChatMessage> {
    return mutex.myWithLock {
      return@myWithLock BaseChatMessageMapper.FromBaseChatMessageData.toBaseChatMessageList(messageHistory.getAll())
    }
  }

  override fun toString(): String {
    return "[chatRoomName: $chatRoomName, roomPasswordHash: $roomPasswordHash, isPublic: $isPublic, createdOn: $createdOn]"
  }

  fun copy(): ChatRoom {
    return ChatRoom(
      chatRoomName,
      chatRoomImageUrl,
      roomPasswordHash?.copyOf(),
      isPublic,
      createdOn,
      userList.toMutableSet(),
      messageHistory.clone()
    )
  }
}