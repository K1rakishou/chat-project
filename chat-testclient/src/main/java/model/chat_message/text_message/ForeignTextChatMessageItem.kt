package model.chat_message.text_message

import model.chat_message.MessageType

class ForeignTextChatMessageItem(
  senderName: String,
  messageText: String
) : TextChatMessageItem(senderName, messageText, -1, FOREIGN_MESSAGE_ID) {

  override fun getMessageType(): MessageType = MessageType.ForeignTextMessage

  override fun toTextMessage(): String {
    return "$senderName: $messageText"
  }
}