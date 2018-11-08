package model.chat_message.text_message

import model.chat_message.MessageType

class SystemChatMessageItem(
  message: String,
  serverMessageId: Int = CLIENT_SIDE_ONLY_MESSAGE_ID,
  clientMessageId: Int = CLIENT_SIDE_ONLY_MESSAGE_ID
) : TextChatMessageItem("System", message, serverMessageId, clientMessageId) {

  override fun getMessageType(): MessageType = MessageType.SystemTextMessage

  override fun toTextMessage(): String {
    return ""
  }
}