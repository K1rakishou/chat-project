package repository.model

import core.model.drainable.chat_message.ChatMessageType

open class BaseChatMessageData(
  val serverMessageId: Int,
  val clientMessageId: Int,
  val messageType: ChatMessageType
)