package store

import core.model.drainable.ChatRoomData
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import model.chat_room_list.BaseChatRoomListItem
import model.chat_room_list.NoRoomsNotificationItem
import model.chat_room_list.SearchChatRoomItem
import utils.ThreadChecker

class SearchChatRoomsStore {
  val searchChatRoomList: ObservableList<BaseChatRoomListItem> = FXCollections.observableArrayList()

  fun reloadSearchChatRoomList(
    foundRooms: List<ChatRoomData>,
    alreadyJoinedRoomsSet: Set<String>
  ) {
    ThreadChecker.throwIfNotOnMainThread()

    val convertedList = foundRooms
      .filterNot { publicChatRoom -> alreadyJoinedRoomsSet.contains(publicChatRoom.chatRoomName) }
      .map { publicChatRoom -> SearchChatRoomItem(publicChatRoom.chatRoomName, publicChatRoom.chatRoomImageUrl) }

    searchChatRoomList.clear()

    if (convertedList.isEmpty()) {
      searchChatRoomList.add(NoRoomsNotificationItem.noRoomsWereFound())
    } else {
      searchChatRoomList.addAll(convertedList)
    }
  }
}