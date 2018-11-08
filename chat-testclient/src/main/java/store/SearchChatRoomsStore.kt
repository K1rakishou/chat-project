package store

import core.model.drainable.ChatRoomData
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import model.chat_room_list.SearchChatRoomItem
import utils.ThreadChecker

class SearchChatRoomsStore {
  val searchChatRoomList: ObservableList<SearchChatRoomItem> = FXCollections.observableArrayList()

  fun reloadSearchChatRoomList(
    foundRooms: List<ChatRoomData>,
    alreadyJoinedRoomsSet: Set<String>
  ) {
    ThreadChecker.throwIfNotOnMainThread()

    val convertedList = foundRooms
      .filterNot { publicChatRoom -> alreadyJoinedRoomsSet.contains(publicChatRoom.chatRoomName) }
      .map { publicChatRoom -> SearchChatRoomItem(publicChatRoom.chatRoomName, publicChatRoom.chatRoomImageUrl) }

    //TODO: if convertedList is empty add some kind of notification that no rooms has been found
    searchChatRoomList.clear()
    searchChatRoomList.addAll(convertedList)
  }
}