package ui.chat_main_window

import controller.ChatMainWindowController
import javafx.geometry.Orientation
import javafx.scene.layout.Border
import javafx.scene.layout.Priority
import tornadofx.View
import tornadofx.splitpane
import tornadofx.vbox
import tornadofx.vboxConstraints

class ChatMainWindow : View("Chat") {
  private val chatMainWindowController: ChatMainWindowController by inject()

  override fun onDock() {
    chatMainWindowController.createController()
  }

  override fun onUndock() {
    chatMainWindowController.destroyController()
  }

  override val root = vbox {
    prefWidth = 720.0
    prefHeight = 480.0

    splitpane {
      orientation = Orientation.HORIZONTAL
      setDividerPositions(0.0)
      vboxConstraints { vGrow = Priority.ALWAYS }

      vbox {
        minWidth = 156.0
        maxWidth = 156.0
        border = Border.EMPTY

        add(ChatRoomListFragment::class)
      }

      vbox {
        border = Border.EMPTY

        add(ChatRoomViewEmpty::class)
      }
    }
  }

}