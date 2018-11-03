package ui.chat_main_window

import ChatApp
import events.ChatMainWindowEvents
import events.ChatRoomListFragmentEvents
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.control.OverrunStyle
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.HBox
import javafx.scene.paint.Paint
import model.chat_room_list.BaseChatRoomListItem
import model.chat_room_list.NoRoomsNotificationItem
import model.chat_room_list.PublicChatRoomItem
import org.fxmisc.flowless.VirtualizedScrollPane
import store.ChatRoomsStore
import tornadofx.*
import ui.base.BaseFragment
import ui.widgets.VirtualListView

class ChatRoomListFragment : BaseFragment() {
  private val chatRoomsStore: ChatRoomsStore by lazy { ChatApp.chatRoomsStore }

  private var lastSelectedChatRoomName: String? = null
  private val rightMargin = 16.0

  private val chatMainWindowSize = params[ChatMainWindow.CHAT_ROOM_LIST_VIEW_SIZE] as ChatMainWindow.ChatRoomViewSizeParams

  init {
    subscribe<ChatRoomListFragmentEvents.SelectItem> { event ->
      virtualListView.selectItem(event.itemIndex)
    }.autoUnsubscribe()
    subscribe<ChatRoomListFragmentEvents.ClearSelection> { _ ->
      virtualListView.clearSelection()
    }.autoUnsubscribe()
  }

  private val virtualListView = VirtualListView(chatRoomsStore.publicChatRoomList, { item ->
    println("Constructing a node for a room with name ${item.roomName}")

    return@VirtualListView when (item) {
      is PublicChatRoomItem -> createCellPublicChatRoomItem(chatMainWindowSize.widthProperty, item)
      is NoRoomsNotificationItem -> createCellNoRoomsNotificationItem(chatMainWindowSize.widthProperty)
      else -> throw RuntimeException("Not implemented for ${item::class}")
    }
  }, { selectedItem ->
    onItemSelected(selectedItem)
  })

  override val root = VirtualizedScrollPane(virtualListView.getVirtualFlow().apply {
    background = Background(BackgroundFill(Paint.valueOf("#ffffff"), CornerRadii.EMPTY, Insets.EMPTY))
    prefHeightProperty().bind(chatMainWindowSize.heightProperty)
  })

  private fun onItemSelected(item: BaseChatRoomListItem) {
    when (item) {
      is PublicChatRoomItem -> {
        val isMyUserAdded = chatRoomsStore.getChatRoomByName(item.roomName)?.isMyUserAdded() ?: false
        if (isMyUserAdded) {
          if (lastSelectedChatRoomName == null || lastSelectedChatRoomName != item.roomName) {
            lastSelectedChatRoomName = item.roomName
            fire(ChatMainWindowEvents.ShowChatRoomViewEvent(item.roomName))
          }
        } else {
          fire(ChatMainWindowEvents.ShowJoinChatRoomDialogEvent(item.roomName))
        }
      }
      is NoRoomsNotificationItem -> {
        fire(ChatMainWindowEvents.ShowCreateChatRoomDialogEvent)
      }
    }
  }

  private fun createCellNoRoomsNotificationItem(widthProperty: ReadOnlyDoubleProperty): HBox {
    return hbox {
      prefHeight = 64.0
      maxHeight = 64.0
      paddingAll = 2.0
      cursor = Cursor.HAND

      prefWidthProperty().bind(widthProperty - rightMargin)
      maxWidthProperty().bind(widthProperty - rightMargin)

      label("No public chat rooms created yet") { minWidth = 8.0 }
    }
  }

  private fun createCellPublicChatRoomItem(widthProperty: ReadOnlyDoubleProperty, item: PublicChatRoomItem): HBox {
    return hbox {
      prefHeight = 64.0
      maxHeight = 64.0
      paddingAll = 2.0
      cursor = Cursor.HAND

      prefWidthProperty().bind(widthProperty - rightMargin)
      maxWidthProperty().bind(widthProperty - rightMargin)

      imageview(item.imageUrl) {
        minHeight = 60.0
        fitHeight = 60.0
        isPreserveRatio = true
        isSmooth = true
        alignment = Pos.CENTER_LEFT
        paddingRight = 8.0
      }
      label { minWidth = 8.0 }
      vbox {
        label(item.roomName) {
          textOverrun = OverrunStyle.ELLIPSIS
        }
        label {
          textProperty().bind(chatRoomsStore.getChatRoomByName(item.roomName)!!.lastMessageProperty)

          textOverrun = OverrunStyle.ELLIPSIS
        }
      }
    }
  }
}