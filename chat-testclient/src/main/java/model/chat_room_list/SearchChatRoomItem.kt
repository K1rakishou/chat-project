package model.chat_room_list

import javafx.beans.property.SimpleStringProperty

class SearchChatRoomItem(
  roomName: String,
  imageUrl: String
) : BaseChatRoomListItem(ChatRoomListItemType.SearchChatRoomItemType, roomName) {

  private val imageUrlProperty by lazy { SimpleStringProperty(imageUrl) }
  fun imageUrlProperty() = imageUrlProperty
  var imageUrl: String
    get() = imageUrlProperty.get()
    set(value) = imageUrlProperty.set(value)
}