package model

import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import model.chat_message.BaseChatMessageItem
import java.util.*

class PublicChatRoomItem(
  roomName: String,
  imageUrl: String,
  userList: ObservableList<PublicUserInChatItem>,
  roomMessages: ObservableList<BaseChatMessageItem>
) {
  val id = UUID.randomUUID()

  private val roomNameProperty by lazy { SimpleStringProperty(roomName) }
  fun roomNameProperty() = roomNameProperty
  var roomName: String
    get() = roomNameProperty.get()
    set(value) = roomNameProperty.set(value)

  private val imageUrlProperty by lazy { SimpleStringProperty(imageUrl) }
  fun imageUrlProperty() = imageUrlProperty
  var imageUrl: String
    get() = imageUrlProperty.get()
    set(value) = imageUrlProperty.set(value)

  private val userListProperty by lazy { SimpleListProperty<PublicUserInChatItem>(userList) }
  fun userListProperty() = userListProperty
  var userList: ObservableList<PublicUserInChatItem>
    get() = userListProperty.get()
    set(value) = userListProperty.set(value)

  private val roomMessagesProperty by lazy { SimpleListProperty<BaseChatMessageItem>(roomMessages) }
  fun roomMessagesProperty() = roomMessagesProperty
  var roomMessages: ObservableList<BaseChatMessageItem>
    get() = roomMessagesProperty.get()
    set(value) = roomMessagesProperty.set(value)

  override fun equals(other: Any?): Boolean {
    if (other == null) {
      return false
    }

    if (other !is PublicChatRoomItem) {
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