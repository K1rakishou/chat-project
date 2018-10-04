package ui.chat_main_window

import controller.ChatRoomListController
import javafx.scene.Parent
import javafx.scene.input.MouseEvent
import tornadofx.View
import tornadofx.listview
import tornadofx.selectedItem

class ChatRoomListView : View() {
  val chatRoomListController: ChatRoomListController by inject()

  override val root = listview(chatRoomListController.chatRooms) {
    cellFragment(ChatRoomListFragment::class)

    addEventFilter(MouseEvent.MOUSE_CLICKED) { event ->
      //I have no idea how to do it without this hack
      if (event.clickCount == 1 && selectedItem != null && (event.target as Parent).id == ChatRoomListFragment.componentId) {
        selectedItem?.let { item ->
          chatRoomListController.joinChatRoom(item)
        }
      }
    }
  }
}