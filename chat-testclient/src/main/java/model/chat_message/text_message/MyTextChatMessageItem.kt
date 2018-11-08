package model.chat_message.text_message

import model.chat_message.MessageType

class MyTextChatMessageItem(
  senderName: String,
  messageText: String,
  serverMessageId: Int = -1,
  clientMessageId: Int = -1
) : TextChatMessageItem(senderName, messageText, serverMessageId, clientMessageId) {

  override fun getMessageType(): MessageType = MessageType.MyTextMessage

  fun isAcceptedByServer(): Boolean = serverMessageId > -1
}