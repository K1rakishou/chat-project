package model.chat_message

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.ItemViewModel
import java.util.*

open class TextChatMessageItem(
  senderName: String,
  messageText: String
) : BaseChatMessageItem() {
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

class TextChatMessageItemModel(
  property: ObjectProperty<TextChatMessageItem>
) : ItemViewModel<TextChatMessageItem>(itemProperty = property) {
  val senderName = bind(autocommit = true) { item?.senderNameProperty() }
  val messageText = bind(autocommit = true) { item?.messageTextProperty() }
}