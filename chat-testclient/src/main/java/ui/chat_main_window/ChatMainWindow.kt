package ui.chat_main_window

import controller.ChatMainWindowController
import events.ChatRoomListClearRoomSelectionEvent
import events.JoinChatRoomInfoEvent
import javafx.geometry.Orientation
import javafx.scene.control.SplitPane
import javafx.scene.layout.Border
import javafx.scene.layout.Priority
import ui.chat_main_window.join_chat_room_dialog.JoinChatRoomDialogFragment
import events.ShowJoinChatRoomDialogEvent
import tornadofx.*

class ChatMainWindow : View("Chat") {
  private val chatMainWindowController: ChatMainWindowController by inject()

  override fun onDock() {
    chatMainWindowController.createController()

    subscribe<ShowJoinChatRoomDialogEvent> {
      runLater {
        val roomNameItem = JoinChatRoomDialogFragment.RoomNameItem(it.roomName)
        val scope = Scope(roomNameItem)
        find<JoinChatRoomDialogFragment>(scope).openModal(resizable = false)
      }
    }
    subscribe<JoinChatRoomInfoEvent> { joinChatRoomInfo ->
      if (!joinChatRoomInfo.canceled) {
        chatMainWindowController.joinChatRoom(joinChatRoomInfo.roomName, joinChatRoomInfo.userName, joinChatRoomInfo.roomPassword)
      } else {
        fire(ChatRoomListClearRoomSelectionEvent)
      }
    }
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
        SplitPane.setResizableWithParent(this, false)

        minWidth = 156.0
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