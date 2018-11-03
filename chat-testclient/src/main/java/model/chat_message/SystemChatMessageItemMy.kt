package model.chat_message

class SystemChatMessageItemMy(
  message: String,
  serverMessageId: Int = CLIENT_SIDE_ONLY_MESSAGE_ID,
  clientMessageId: Int = CLIENT_SIDE_ONLY_MESSAGE_ID
) : MyTextChatMessageItem("System", message, serverMessageId, clientMessageId) {

  override fun getMessageType(): MessageType = MessageType.SystemTextMessage

  override fun toTextMessage(): String {
    return ""
  }
}