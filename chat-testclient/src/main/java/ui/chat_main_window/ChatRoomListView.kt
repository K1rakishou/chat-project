package ui.chat_main_window

import controller.ChatRoomListController
import tornadofx.View
import tornadofx.listview

class ChatRoomListView : View() {
  val chatRoomListController: ChatRoomListController by inject()

  override val root = listview(chatRoomListController.chatRooms) {
    cellFragment(ChatRoomListFragment::class)
  }
}