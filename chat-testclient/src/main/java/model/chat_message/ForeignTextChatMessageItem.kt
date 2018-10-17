package model.chat_message

class ForeignTextChatMessageItem(
  senderName: String,
  messageText: String
) : MyTextChatMessageItem(senderName, messageText, -1, FOREIGN_MESSAGE_ID) {

  override fun getMessageType(): MessageType = MessageType.ForeignTextMessage
}