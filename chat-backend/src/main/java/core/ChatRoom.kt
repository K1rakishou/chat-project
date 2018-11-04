package core

import core.collections.RingBuffer
import core.extensions.myWithLock
import core.model.drainable.chat_message.BaseChatMessage
import core.model.drainable.chat_message.ChatMessageType
import kotlinx.coroutines.sync.Mutex
import repository.mapper.BaseChatMessageMapper
import repository.model.BaseChatMessageData
import java.util.concurrent.atomic.AtomicInteger

data class ChatRoom(
  val chatRoomName: String,
  val chatRoomImageUrl: String,
  val roomPasswordHash: String?,
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

  suspend fun addMessage(
    clientId: String,
    messageType: ChatMessageType,
    clientMessageId: Int,
    senderName: String,
    messageText: String
  ): Int {
    val serverMessageId = messageIdCounter.getAndIncrement()
    val chatMessageData = BaseChatMessageMapper.FromBaseChatMessage.toBaseChatMessageData(
      clientId,
      serverMessageId,
      messageType,
      clientMessageId,
      senderName,
      messageText
    )

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

  suspend fun passwordsMatch(chatRoomPassword: String): Boolean {
    return mutex.myWithLock {
      println("Room password ($roomPasswordHash), user password ($chatRoomPassword)")
      return@myWithLock roomPasswordHash == chatRoomPassword
    }
  }

  suspend fun getMessageHistory(clientId: String): List<BaseChatMessage> {
    return mutex.myWithLock {
      return@myWithLock BaseChatMessageMapper.FromBaseChatMessageData
        .toBaseChatMessageList(clientId, messageHistory.getAll())
    }
  }

  override fun toString(): String {
    return "[chatRoomName: $chatRoomName, roomPasswordHash: $roomPasswordHash, isPublic: $isPublic, createdOn: $createdOn]"
  }

  fun copy(): ChatRoom {
    return ChatRoom(
      chatRoomName,
      chatRoomImageUrl,
      roomPasswordHash,
      isPublic,
      createdOn,
      userList.toMutableSet(),
      messageHistory.clone()
    )
  }
}