package ui.chat_main_window

import javafx.scene.layout.Priority
import model.PublicChatRoomItem
import model.PublicChatRoomItemModel
import tornadofx.*

class ChatRoomListCellFragment : ListCellFragment<PublicChatRoomItem>() {
  val chatRoom = PublicChatRoomItemModel(itemProperty)

  override val root = hbox {
    id = componentId
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

  companion object {
    const val componentId = "PublicChatRoomItemCellFragment"
  }
}