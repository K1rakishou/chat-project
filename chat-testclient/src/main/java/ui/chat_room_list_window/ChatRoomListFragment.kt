package ui.chat_room_list_window

import javafx.geometry.Insets
import javafx.scene.layout.Priority
import model.PublicChatRoomItem
import model.PublicChatRoomItemModel
import tornadofx.*

class ChatRoomListFragment : ListCellFragment<PublicChatRoomItem>() {
  val chatRoom = PublicChatRoomItemModel(itemProperty)

  override val root = hbox {
    label(chatRoom.usersCount) {
    }
    pane {
      padding = Insets(0.0, 15.0, 0.0, 0.0)
    }
    label(chatRoom.roomName) {
      hgrow = Priority.ALWAYS
    }
  }
}