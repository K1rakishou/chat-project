package model.chat_message.text_message

import core.LinksExtractor
import javafx.beans.property.SimpleStringProperty
import model.chat_message.BaseChatMessageItem
import java.util.*

abstract class TextChatMessageItem(
  senderName: String,
  messageText: String,
  serverMessageId: Int = -1,
  clientMessageId: Int = -1
) : BaseChatMessageItem(serverMessageId, clientMessageId) {
  override fun toTextMessage(): String {
    return "$senderName: $messageText"
  }

  val links = LinksExtractor.extract(messageText)
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

    if (other !is TextChatMessageItem) {
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