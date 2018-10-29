package ui.chat_main_window

import ChatApp
import controller.ChatMainWindowController
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
  private val store: ChatRoomsStore by lazy { ChatApp.chatRoomsStore }
  private val controller: ChatMainWindowController by inject()
  private val chatMainWindowSize: ChatMainWindow.ChatRoomListFragmentParams by inject()
  private val rightMargin = 16.0

  init {
    subscribe<ChatRoomListFragmentEvents.SelectItem> { event ->
      virtualListView.selectItem(event.itemIndex)
    }.autoUnsubscribe()
    subscribe<ChatRoomListFragmentEvents.ClearSelection> { _ ->
      virtualListView.clearSelection()
    }.autoUnsubscribe()
  }

  private val virtualListView = VirtualListView(store.publicChatRoomList, { item ->
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
        controller.updateSelectedRoom(item.roomName)

        val isMyUserAdded = store.getChatRoomByName(item.roomName)?.isMyUserAdded() ?: false
        if (isMyUserAdded) {
          val selectedItem =  virtualListView.getSelectedItem()
          if (selectedItem == null || selectedItem.roomName != item.roomName) {
            controller.reloadRoomMessageHistory(item.roomName)
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
//        label(controller.lastChatMessageMap[item.roomName]) {
//          textOverrun = OverrunStyle.ELLIPSIS
//        }
      }
    }
  }
}