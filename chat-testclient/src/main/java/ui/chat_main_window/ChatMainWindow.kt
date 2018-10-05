package ui.chat_main_window

import javafx.geometry.Orientation
import javafx.scene.layout.Border
import javafx.scene.layout.Priority
import tornadofx.*

class ChatMainWindow : View("Chat") {

  override val root = vbox {
    splitpane {
      orientation = Orientation.HORIZONTAL
      setDividerPositions(0.3)
      vboxConstraints { vGrow = Priority.ALWAYS }

      borderpane {
        border = Border.EMPTY
        center(ChatRoomListFragment::class)
      }

      borderpane {
        border = Border.EMPTY
        center(ChatRoomViewEmpty::class)
      }
    }
  }

}