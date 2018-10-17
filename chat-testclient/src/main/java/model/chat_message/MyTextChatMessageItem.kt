package model.chat_message

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.ItemViewModel
import java.util.*

open class MyTextChatMessageItem(
  senderName: String,
  messageText: String,
  serverMessageId: Int = -1,
  clientMessageId: Int = -1
) : BaseChatMessageItem(serverMessageId, clientMessageId) {

  override fun getMessageType(): MessageType = MessageType.MyTextMessage

  val id = UUID.randomUUID()

  private val senderNameProperty by lazy { SimpleStringProperty(senderName) }
  fun senderNameProperty() = senderNameProperty
  var senderName: String
    get() = senderNameProperty.get()
    set(value) = senderNameProperty.set(value)

  private val messageTextProperty by lazy { SimpleStringProperty(messageText) }
  fun messageTextProperty() = messageTextProperty
  var messageText: String
    get() = messageTextProperty.get()
    set(value) = messageTextProperty.set(value)

  override fun equals(other: Any?): Boolean {
    if (other == null) {
      return false
    }

    if (other !is MyTextChatMessageItem) {
      return false
    }

    if (this === other) {
      return true
    }

    return id == other.id
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }
}