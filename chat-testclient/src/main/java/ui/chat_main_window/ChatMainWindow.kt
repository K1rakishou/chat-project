package ui.chat_main_window

import ChatApp.Companion.settingsStore
import controller.ChatMainWindowController
import events.ChatMainWindowEvents
import events.ChatRoomListFragmentEvents
import events.ChatRoomViewEvents
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Orientation
import javafx.scene.control.SplitPane
import javafx.scene.layout.Border
import javafx.scene.layout.Priority
import kotlinx.coroutines.delay
import store.settings.ChatMainWindowSettings
import tornadofx.*
import ui.base.BaseView
import ui.chat_main_window.create_chat_room_dialog.CreateChatRoomDialogFragment
import ui.chat_main_window.join_chat_room_dialog.JoinChatRoomDialogFragment
import java.lang.IllegalStateException

class ChatMainWindow : BaseView("Chat") {
  private val delayUntilViewShown = 100L

  private val controller: ChatMainWindowController by inject()
  private val chatMainWindowSettings: ChatMainWindowSettings by lazy { ChatApp.settingsStore.chatMainWindowSettings }

  private val selectedRoomNameProperty = SimpleStringProperty()
  private val chatRoomViewSizeParams = ChatRoomViewSizeParams(SimpleDoubleProperty(), SimpleDoubleProperty())
  private val chatRoomListViewSizeParams = ChatRoomViewSizeParams(SimpleDoubleProperty(), SimpleDoubleProperty())

  init {
    subscribe<ChatMainWindowEvents.JoinedChatRoomEvent> { event ->
      controller.onJoinedToChatRoom(event.roomName, event.userName, event.users, event.messageHistory)
    }.autoUnsubscribe()
    subscribe<ChatMainWindowEvents.ShowJoinChatRoomDialogEvent> { event ->
      val roomNameItem = JoinChatRoomDialogFragment.RoomNameItem(event.roomName)
      val scope = Scope(roomNameItem)
      find<JoinChatRoomDialogFragment>(scope).openModal(resizable = false)
    }.autoUnsubscribe()
    subscribe<ChatMainWindowEvents.ShowCreateChatRoomDialogEvent> {
      showCreateChatRoomDialog()
    }
    subscribe<ChatMainWindowEvents.ChatRoomCreatedEvent> { event ->
      controller.onChatRoomCreated(event.roomName, event.userName, event.roomImageUrl)
    }.autoUnsubscribe()
    subscribe<ChatMainWindowEvents.ShowChatRoomViewEvent> { event ->
      showChatRoomView(event.selectedRoomName)
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
              showCreateChatRoomDialog()
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

          chatRoomListViewSizeParams.widthProperty.bind(this@vbox.widthProperty())
          chatRoomListViewSizeParams.heightProperty.bind(this@vbox.heightProperty())

          val parameters = mutableMapOf(
            CHAT_ROOM_LIST_VIEW_SIZE to chatRoomListViewSizeParams
          )

          add(find<ChatRoomListFragment>(params = parameters))
        }

        vbox {
          border = Border.EMPTY
          chatRoomViewSizeParams.widthProperty.bind(this@vbox.widthProperty())
          chatRoomViewSizeParams.heightProperty.bind(this@vbox.heightProperty())

          add(ChatRoomViewEmpty::class)
        }
      }
    }
  }

  fun selectRoomWithName(roomName: String) {
    doOnUI {
      fire(ChatRoomListFragmentEvents.SelectItem(roomName))
    }
  }

  private fun showCreateChatRoomDialog() {
    doOnUI {
      find<CreateChatRoomDialogFragment>().openModal(resizable = false)
    }
  }

  fun showChatRoomView(roomName: String) {
    doOnUI {
      selectedRoomNameProperty.set(roomName)

      val parameters = mutableMapOf(
        CHAT_ROOM_VIEW_SIZE to chatRoomViewSizeParams
      )

      val chatRoomViewEmpty = find<ChatRoomViewEmpty>()
      if (chatRoomViewEmpty.isDocked) {
        chatRoomViewEmpty.replaceWith(find<ChatRoomView>(params = parameters))
      }

      delay(delayUntilViewShown)

      if (find<ChatRoomView>().isDocked) {
        fire(ChatRoomViewEvents.ChangeSelectedRoom(roomName))
      } else {
        throw IllegalStateException("Neither ChatRoomViewEmpty nor ChatRoomView is docked. Should not happen.")
      }
    }
  }

  class ChatRoomViewSizeParams(
    val widthProperty: SimpleDoubleProperty,
    val heightProperty: SimpleDoubleProperty
  )

  companion object {
    const val CHAT_ROOM_VIEW_SIZE = "chat_room_view_size"
    const val CHAT_ROOM_LIST_VIEW_SIZE = "chat_room_list_view_size"
  }
}