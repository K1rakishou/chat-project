package ui.chat_room_list_window

import javafx.geometry.Insets
import javafx.scene.control.Alert
import tornadofx.*

class ChatRoomListFooter : View() {

  override val root = vbox {
    hbox {
      padding = Insets(8.0)

      button("Create") {
        setOnAction {
          alert(Alert.AlertType.INFORMATION, "Create pressed")
        }
      }
      button("Join") {
        setOnAction {
          alert(Alert.AlertType.INFORMATION, "Join pressed")
        }
      }
    }
  }
}