package repository.model

import core.model.drainable.chat_message.ChatMessageType

class TextChatMessageData(
  serverMessageId: Int,
  clientMessageId: Int,
  val senderName: String,
  val message: String
) : BaseChatMessageData(serverMessageId, clientMessageId, ChatMessageType.Text)