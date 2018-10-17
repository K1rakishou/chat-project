package model.chat_message

import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

abstract class BaseChatMessageItem(
  val serverMessageId: Int,
  val clientMessageId: Int
) {

  abstract fun getMessageType(): MessageType

  fun shouldUpdateIds(): Boolean {
    return when (getMessageType()) {
      MessageType.MyTextMessage -> true
      MessageType.SystemTextMessage,
      MessageType.ForeignTextMessage -> false
      else -> throw IllegalStateException("Not implemented for ${this::class}")
    }
  }

  enum class MessageType {
    MyTextMessage,
    ForeignTextMessage,
    SystemTextMessage
  }

  companion object {
    const val CLIENT_SIDE_ONLY_MESSAGE_ID = -1
    const val FOREIGN_MESSAGE_ID = -2

    fun copyWithNewClientMessageId(oldChatMessageItem: BaseChatMessageItem, clientMessage: Int): BaseChatMessageItem {
      return when (oldChatMessageItem) {
        is MyTextChatMessageItem -> MyTextChatMessageItem(
          oldChatMessageItem.senderName,
          oldChatMessageItem.messageText,
          oldChatMessageItem.serverMessageId,
          clientMessage
        )
        is SystemChatMessageItemMy -> SystemChatMessageItemMy(
          oldChatMessageItem.messageText,
          oldChatMessageItem.serverMessageId,
          clientMessage
        )
        else -> throw IllegalArgumentException("Not implemented for ${oldChatMessageItem::class}")
      }
    }

    fun copyWithNewServerMessageId(oldChatMessageItem: BaseChatMessageItem, serverMessageId: Int): BaseChatMessageItem {
      return when (oldChatMessageItem) {
        is MyTextChatMessageItem -> MyTextChatMessageItem(
          oldChatMessageItem.senderName,
          oldChatMessageItem.messageText,
          serverMessageId,
          oldChatMessageItem.clientMessageId
        )
        is SystemChatMessageItemMy -> SystemChatMessageItemMy(
          oldChatMessageItem.messageText,
          serverMessageId,
          oldChatMessageItem.clientMessageId
        )
        else -> throw IllegalArgumentException("Not implemented for ${oldChatMessageItem::class}")
      }
    }
  }
}