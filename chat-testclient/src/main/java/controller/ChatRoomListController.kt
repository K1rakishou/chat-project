package controller

import javafx.collections.FXCollections
import model.PublicChatRoomItem
import tornadofx.Controller

class ChatRoomListController : Controller() {
  val chatRooms = FXCollections.observableArrayList<PublicChatRoomItem>()

  val store: Store by inject()

  init {
    chatRooms.addAll(store.getChatRoomList().map { PublicChatRoomItem(it.roomName, it.usersCount) })
  }
}