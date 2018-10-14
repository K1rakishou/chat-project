package ui.chat_main_window

import javafx.geometry.Pos
import javafx.scene.layout.Priority
import tornadofx.*

class ChatRoomViewEmpty : View() {

  override val root = vbox {
    vboxConstraints { vGrow = Priority.ALWAYS }
    alignment = Pos.CENTER

    label("Select chat room to join") {
      addClass(Styles.chatRoomViewEmptyLabel)
    }
  }
}