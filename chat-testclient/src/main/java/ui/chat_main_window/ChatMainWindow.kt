package ui.chat_main_window

import controller.ChatMainWindowController
import events.ChatMainWindowEvents
import javafx.geometry.Orientation
import javafx.scene.control.SplitPane
import javafx.scene.layout.Border
import javafx.scene.layout.Priority
import tornadofx.*
import ui.base.BaseView
import ui.chat_main_window.join_chat_room_dialog.JoinChatRoomDialogFragment

class ChatMainWindow : BaseView("Chat") {
  private val controller: ChatMainWindowController by inject()

  init {
    subscribe<ChatMainWindowEvents.JoinedChatRoomEvent> { event ->
      controller.loadRoomInfo(event.roomName, event.userName, event.users, event.messageHistory)
    }.autoUnsubscribe()

    subscribe<ChatMainWindowEvents.ShowJoinChatRoomDialogEvent> { event ->
      val roomNameItem = JoinChatRoomDialogFragment.RoomNameItem(event.roomName)
      val scope = Scope(roomNameItem)
      find<JoinChatRoomDialogFragment>(scope).openModal(resizable = false)
    }.autoUnsubscribe()
  }

  override fun onDock() {
    controller.createController(this)
  }

  override fun onUndock() {
    controller.destroyController()
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