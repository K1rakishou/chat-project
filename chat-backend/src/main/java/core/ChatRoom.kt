package core

import core.collections.RingBuffer
import core.model.drainable.chat_message.BaseChatMessage
import core.model.drainable.chat_message.ChatMessageType
import repository.mapper.BaseChatMessageMapper
import repository.model.BaseChatMessageData
import java.util.concurrent.atomic.AtomicInteger

data class ChatRoom(
  val chatRoomName: String,
  val chatRoomImageUrl: String,
  val roomPasswordHash: String?,
  val isPublic: Boolean,
  val createdOn: Long,
  val userList: MutableSet<User> = mutableSetOf(),
  val messageHistory: RingBuffer<BaseChatMessageData> = RingBuffer(Constants.maxRoomHistoryMessagesCount)
) {

  private val messageIdCounter = AtomicInteger(0)

  fun addUser(userInRoom: User): Boolean {
    return userList.add(userInRoom)
  }

  fun removeUser(userName: String) {
    userList.removeIf { it.userName == userName }
  }

  fun addMessage(
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

    messageHistory.add(chatMessageData)
    return serverMessageId
  }

  fun containsUser(userName: String): Boolean {
    return userList
      .firstOrNull { it.userName == userName }
      ?.let { true } ?: false
  }

  fun getUser(userName: String): User? {
    return userList.firstOrNull { it.userName == userName }
  }

  fun getEveryone(): List<User> {
    return userList.toMutableList()
  }

  fun getEveryoneExcept(userName: String): List<User> {
    return mutableListOf<User>()
      .apply {
        addAll(userList.filter { it.userName != userName })
      }
  }

  fun countUsers(): Int {
    return userList.size
  }

  fun hasPassword(): Boolean {
    return roomPasswordHash != null
  }

  fun passwordsMatch(chatRoomPassword: String): Boolean {
    println("Room password ($roomPasswordHash), user password ($chatRoomPassword)")
    return roomPasswordHash == chatRoomPassword
  }

  fun getMessageHistory(clientId: String): List<BaseChatMessage> {
    return BaseChatMessageMapper.FromBaseChatMessageData
      .toBaseChatMessageList(clientId, messageHistory.getAll())
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