package model.chat_message

import model.chat_message.text_message.MyTextChatMessageItem
import model.chat_message.text_message.SystemChatMessageItem

abstract class BaseChatMessageItem(
  val serverMessageId: Int,
  val clientMessageId: Int
) {

  abstract fun getMessageType(): MessageType

  abstract fun toTextMessage(): String

  fun shouldUpdateIds(): Boolean {
    return when (getMessageType()) {
      MessageType.MyTextMessage,
      MessageType.MyImageMessage -> true
      MessageType.SystemTextMessage,
      MessageType.ForeignTextMessage -> false
      else -> throw IllegalStateException("Not implemented for ${this::class}")
    }
  }

  fun canBeUsedAsLastMessage(): Boolean {
    return when (getMessageType()) {
      MessageType.MyTextMessage,
      MessageType.ForeignTextMessage ,
      MessageType.MyImageMessage -> true
      MessageType.SystemTextMessage -> false
    }
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
        is SystemChatMessageItem -> SystemChatMessageItem(
          oldChatMessageItem.messageText,
          oldChatMessageItem.serverMessageId,
          clientMessage
        )
        is MyImageChatMessage -> MyImageChatMessage(
          oldChatMessageItem.senderName,
          oldChatMessageItem.imageFile,
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
        is SystemChatMessageItem -> SystemChatMessageItem(
          oldChatMessageItem.messageText,
          serverMessageId,
          oldChatMessageItem.clientMessageId
        )
        else -> throw IllegalArgumentException("Not implemented for ${oldChatMessageItem::class}")
      }
    }
  }
}