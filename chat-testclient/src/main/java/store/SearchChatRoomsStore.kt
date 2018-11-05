package store

import core.model.drainable.ChatRoomData
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import model.chat_room_list.SearchChatRoomItem
import utils.ThreadChecker

class SearchChatRoomsStore {
  val searchChatRoomList: ObservableList<SearchChatRoomItem> = FXCollections.observableArrayList()

  fun reloadSearchChatRoomList(foundRooms: List<ChatRoomData>) {
    ThreadChecker.throwIfNotOnMainThread()

    val converted = foundRooms.map { publicChatRoom ->
      SearchChatRoomItem(publicChatRoom.chatRoomName, publicChatRoom.chatRoomImageUrl)
    }

    searchChatRoomList.clear()
    searchChatRoomList.addAll(converted)
  }
}