package ui.chat_room_list_window

import tornadofx.View
import tornadofx.borderpane

class ChatRoomListWindow : View() {

  override val root = borderpane {
    center(ChatRoomListView::class)
  }

}