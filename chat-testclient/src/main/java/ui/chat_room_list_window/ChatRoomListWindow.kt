package ui.chat_room_list_window

import javafx.scene.layout.Border
import tornadofx.View
import tornadofx.borderpane

class ChatRoomListWindow : View() {

  override val root = borderpane {
    setPrefSize(384.0, 720.0)
    border = Border.EMPTY

    top(ChatRoomListHeader::class)
    center(ChatRoomListView::class)
    bottom(ChatRoomListFooter::class)
  }

}