package ui.chat_main_window

import javafx.geometry.Pos
import tornadofx.*

class ChatRoomViewEmpty : Fragment() {

  override val root = vbox {
    alignment = Pos.CENTER

    label("Select chat room to display it") {
      addClass(Styles.chatRoomViewEmptyLabel)
    }
  }
}