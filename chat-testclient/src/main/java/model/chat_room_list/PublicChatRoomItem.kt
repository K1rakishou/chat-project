package model.chat_room_list

import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import model.PublicUserInChatItem
import model.chat_message.BaseChatMessageItem

class PublicChatRoomItem(
  roomName: String,
  imageUrl: String,
  userList: ObservableList<PublicUserInChatItem>,
  roomMessages: ObservableList<BaseChatMessageItem>
) : BaseChatRoomListItem(ChatRoomListItemType.ChatRoomItemType) {
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

  companion object {
    fun create(roomName: String, roomImageUrl: String): PublicChatRoomItem {
      return PublicChatRoomItem(roomName, roomImageUrl, FXCollections.observableArrayList(), FXCollections.observableArrayList())
    }
  }
}