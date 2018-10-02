package ui.chat_main_window

import javafx.scene.layout.Priority
import model.PublicChatRoomItem
import model.PublicChatRoomItemModel
import tornadofx.*

class ChatRoomListFragment : ListCellFragment<PublicChatRoomItem>() {
  val chatRoom = PublicChatRoomItemModel(itemProperty)

  override val root = hbox {
    prefHeight = 96.0

    label(chatRoom.usersCount) {
    }
    pane {
      paddingRight = 15.0
    }
    label(chatRoom.roomName) {
      hgrow = Priority.ALWAYS
    }
  }
}