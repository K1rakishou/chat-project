package ui.chat_main_window

import javafx.geometry.Pos
import tornadofx.*

class ChatRoomViewEmpty : View() {

  override val root = vbox {
    alignment = Pos.CENTER

    label("Select chat room to display it") {
      addClass(Styles.chatRoomViewEmptyLabel)
    }
  }
}