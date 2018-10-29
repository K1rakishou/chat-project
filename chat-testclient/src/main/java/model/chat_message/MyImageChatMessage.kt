package model.chat_message

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import java.io.File
import java.util.*

class MyImageChatMessage(
  senderName: String,
  imageFile: File,
  serverMessageId: Int = -1,
  clientMessageId: Int = -1
) : BaseChatMessageItem(serverMessageId, clientMessageId) {

  override fun getMessageType(): MessageType = MessageType.MyImageMessage

  val id = UUID.randomUUID()

  private val senderNameProperty by lazy { SimpleStringProperty(senderName) }
  fun senderNameProperty() = senderNameProperty
  var senderName: String
    get() = senderNameProperty.get()
    set(value) = senderNameProperty.set(value)

  private val imageFileProperty by lazy { SimpleObjectProperty<File>(imageFile) }
  fun imageUrlProperty() = imageFileProperty
  var imageFile: File
    get() = imageFileProperty.get()
    set(value) = imageFileProperty.set(value)

  override fun equals(other: Any?): Boolean {
    if (other == null) {
      return false
    }

    if (other !is MyImageChatMessage) {
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