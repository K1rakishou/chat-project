package model.chat_room_list

import javafx.beans.property.SimpleStringProperty

abstract class BaseChatRoomListItem(
  val type: ChatRoomListItemType,
  chatRoomName: String
) {

  private val roomNameProperty by lazy { SimpleStringProperty(chatRoomName) }
  fun roomNameProperty() = roomNameProperty
  var roomName: String
    get() = roomNameProperty.get()
    set(value) = roomNameProperty.set(value)

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

    return roomName == other.roomName
  }

  override fun hashCode(): Int {
    return roomName.hashCode()
  }

  enum class ChatRoomListItemType {
    ChatRoomItemType,
    NoRoomsNotificationType,
    SearchChatRoomItemType
  }
}