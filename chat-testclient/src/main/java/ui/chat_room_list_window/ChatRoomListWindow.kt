package ui.chat_room_list_window

import javafx.scene.layout.Border
import tornadofx.View
import tornadofx.borderpane
import ui.loading_window.LoadingWindow

class ChatRoomListWindow : View() {
  val loadingView: LoadingWindow by inject()

  override fun onDock() {
    super.onDock()

    loadingView.openModal()
  }

  override val root = borderpane {
    border = Border.EMPTY

    top(ChatRoomListHeader::class)
    center(ChatRoomListView::class)
    bottom(ChatRoomListFooter::class)
  }

}