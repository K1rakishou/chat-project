package ui.chat_main_window

import ChatApp.Companion.settingsStore
import controller.ChatMainWindowController
import events.ChatMainWindowEvents
import events.ChatRoomListFragmentEvents
import javafx.geometry.Orientation
import javafx.scene.control.SplitPane
import javafx.scene.layout.Border
import javafx.scene.layout.Priority
import store.settings.ChatMainWindowSettings
import tornadofx.*
import ui.base.BaseView
import ui.chat_main_window.create_chat_room_dialog.CreateChatRoomDialogFragment
import ui.chat_main_window.join_chat_room_dialog.JoinChatRoomDialogFragment

class ChatMainWindow : BaseView("Chat") {
  private val controller: ChatMainWindowController by inject()
  private val chatMainWindowSettings: ChatMainWindowSettings by lazy { settingsStore.chatMainWindowSettings }

  init {
    subscribe<ChatMainWindowEvents.JoinedChatRoomEvent> { event ->
      controller.onJoinedToChatRoom(event.roomName, event.userName, event.users, event.messageHistory)
    }.autoUnsubscribe()

    subscribe<ChatMainWindowEvents.ShowJoinChatRoomDialogEvent> { event ->
      val roomNameItem = JoinChatRoomDialogFragment.RoomNameItem(event.roomName)
      val scope = Scope(roomNameItem)
      find<JoinChatRoomDialogFragment>(scope).openModal(resizable = false)
    }.autoUnsubscribe()

    subscribe<ChatMainWindowEvents.ChatRoomCreatedEvent> { event ->
      controller.onChatRoomCreated(event.roomName, event.userName, event.roomImageUrl)
    }.autoUnsubscribe()
  }

  override fun onDock() {
    controller.createController(this)

    currentWindow?.x = chatMainWindowSettings.windowXposition
    currentWindow?.y = chatMainWindowSettings.windowYposition
  }

  override fun onUndock() {
    chatMainWindowSettings.updateWindowXposition(currentWindow?.x)
    chatMainWindowSettings.updateWindowYposition(currentWindow?.y)
    chatMainWindowSettings.updateWindowHeight(root.height)
    chatMainWindowSettings.updateWindowWidth(root.width)

    settingsStore.save()
    controller.destroyController()
  }

  override val root = borderpane {
    prefWidth = chatMainWindowSettings.windowWidth
    prefHeight = chatMainWindowSettings.windowHeight

    top {
      menubar {
        menu("Chat") {
          item("Create Chat Room") {
            action {
              find<CreateChatRoomDialogFragment>().openModal(resizable = false)
            }
          }
        }
      }
    }

    center {
      splitpane {
        orientation = Orientation.HORIZONTAL
        setDividerPositions(0.0)
        vboxConstraints { vGrow = Priority.ALWAYS }

        vbox {
          SplitPane.setResizableWithParent(this, false)

          minWidth = 200.0
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

  fun selectRoomWithIndex(index: Int) {
    doOnUI {
      fire(ChatRoomListFragmentEvents.SelectListViewItem(index))
    }
  }
}