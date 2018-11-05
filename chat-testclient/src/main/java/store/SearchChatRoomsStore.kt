package store

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import model.chat_room_list.BaseChatRoomListItem

class SearchChatRoomsStore {
  val searchChatRoomList: ObservableList<BaseChatRoomListItem> = FXCollections.observableArrayList()
}