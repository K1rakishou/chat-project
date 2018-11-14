package ui.chat_main_window

import ChatApp
import ChatApp.Companion.settingsStore
import controller.ChatMainWindowController
import events.ChatMainWindowEvents
import events.ChatRoomListFragmentEvents
import events.ChatRoomViewEvents
import javafx.beans.property.SimpleDoubleProperty
import javafx.geometry.Orientation
import javafx.scene.control.SplitPane
import javafx.scene.layout.Border
import javafx.scene.layout.Priority
import store.SelectedRoomStore
import store.settings.ChatMainWindowSettings
import tornadofx.*
import ui.base.BaseView
import ui.chat_main_window.create_chat_room_dialog.CreateChatRoomDialogFragment
import ui.chat_main_window.join_chat_room_dialog.JoinChatRoomDialogFragment

class ChatMainWindow : BaseView("Chat") {
  private val leftPaneMinWidth = 230.0

  private val controller: ChatMainWindowController by inject()
  private val chatMainWindowSettings: ChatMainWindowSettings by lazy { ChatApp.settingsStore.chatMainWindowSettings }
  private val selectedRoomStore: SelectedRoomStore by lazy { ChatApp.selectedRoomStore }

  private val chatRoomViewSizeParams = ChatRoomViewSizeParams(SimpleDoubleProperty(), SimpleDoubleProperty())
  private val chatRoomListViewSizeParams = ChatRoomViewSizeParams(SimpleDoubleProperty(), SimpleDoubleProperty())

  init {
    subscribe<ChatMainWindowEvents.JoinedChatRoomEvent> { event ->
      controller.onJoinedToChatRoom(event.roomName, event.roomImageUrl, event.userName, event.users, event.messageHistory)
    }.autoUnsubscribe()
    subscribe<ChatMainWindowEvents.ShowJoinChatRoomDialogEvent> { event ->
      val roomNameItem = JoinChatRoomDialogFragment.RoomNameItem(event.roomName)
      val scope = Scope(roomNameItem)
      find<JoinChatRoomDialogFragment>(scope).openModal(resizable = false)
    }.autoUnsubscribe()
    subscribe<ChatMainWindowEvents.ShowCreateChatRoomDialogEvent> {
      showCreateChatRoomDialog()
    }.autoUnsubscribe()
    subscribe<ChatMainWindowEvents.ChatRoomCreatedEvent> { event ->
      controller.onChatRoomCreated(event.roomName, event.userName, event.roomImageUrl)
    }.autoUnsubscribe()
    subscribe<ChatMainWindowEvents.ShowChatRoomViewEvent> { event ->
      showChatRoomView(event.selectedRoomName)
      selectRoomWithName(event.selectedRoomName)
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
    addClass(Styles.chatMainWindow)

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
        addClass(Styles.splitpane)

        orientation = Orientation.HORIZONTAL
        setDividerPositions(0.0)
        vboxConstraints { vGrow = Priority.ALWAYS }

        vbox {
          SplitPane.setResizableWithParent(this, false)

          minWidth = leftPaneMinWidth
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

  private fun showCreateChatRoomDialog() {
    doOnUI {
      find<CreateChatRoomDialogFragment>().openModal(resizable = false)
    }
  }

  fun showChatRoomView(roomName: String) {
    doOnUI {
      title = "Chat [${roomName}]"

      val parameters = mutableMapOf(
        CHAT_ROOM_VIEW_SIZE to chatRoomViewSizeParams
      )

      val chatRoomViewEmpty = find<ChatRoomViewEmpty>()
      if (chatRoomViewEmpty.isDocked) {
        chatRoomViewEmpty.replaceWith(find<ChatRoomView>(params = parameters))
      }
    }
  }

  fun onJoinedChatRoom(roomName: String) {
    //update room selection
    fire(ChatRoomListFragmentEvents.ClearSearchInput)
    selectRoomWithName(roomName)
  }

  fun selectRoomWithName(roomName: String) {
    doOnUI {
      selectedRoomStore.setSelectedRoom(roomName)
    }
  }

  fun scrollChatMessagesToBottom() {
    doOnUI {
      fire(ChatRoomViewEvents.ScrollToBottom)
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