package ui.chat_main_window

import controller.ChatMainWindowController
import events.ChatMainWindowEvents
import events.ChatRoomListFragmentEvents
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.input.MouseButton
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import model.chat_room_list.BaseChatRoomListItem
import model.chat_room_list.NoRoomsNotificationItem
import model.chat_room_list.PublicChatRoomItem
import store.ChatRoomsStore
import tornadofx.*
import ui.base.BaseFragment
import java.lang.RuntimeException

class ChatRoomListFragment : BaseFragment() {
  private val store: ChatRoomsStore by lazy { ChatApp.chatRoomsStore }
  private val controller: ChatMainWindowController by inject()
  private val rightMargin = 16.0

  private var chatRoomListView: ListView<BaseChatRoomListItem>? = null

  init {
    subscribe<ChatRoomListFragmentEvents.SelectListViewItem> { event ->
      chatRoomListView?.selectionModel?.select(event.itemIndex)
    }.autoUnsubscribe()
  }

  override val root = listview(store.publicChatRoomList) {
    vboxConstraints { vGrow = Priority.ALWAYS }

    setCellFactory { cellFactory(widthProperty()) }
    chatRoomListView = this
  }

  private fun cellFactory(widthProperty: ReadOnlyDoubleProperty): ListCell<BaseChatRoomListItem> {
    val cell = object : ListCell<BaseChatRoomListItem>() {
      override fun updateItem(item: BaseChatRoomListItem?, empty: Boolean) {
        super.updateItem(item, empty)

        if (item == null) {
          graphic = null
          return
        }

        //TODO: Optimize.
        //This method is getting called every time a ListView item is clicked.
        graphic = when (item) {
          is PublicChatRoomItem -> createCellPublicChatRoomItem(widthProperty, item)
          is NoRoomsNotificationItem -> createCellNoRoomsNotificationItem(widthProperty)
          else -> throw RuntimeException("Not implemented for ${item::class}")
        }
      }
    }

    cell.id = componentId

    cell.setOnMouseClicked { event ->
      if (cell.isEmpty) {
        event.consume()
        return@setOnMouseClicked
      }

      val target = event.target as? Node
        ?: return@setOnMouseClicked

      if (event.button == MouseButton.PRIMARY && event.clickCount == 1) {
        val (item, shouldReloadRoomMessageHistory) = if (target.id == componentId) {
          //This happens normally when not selected ListView item gets selected by user
          (target as? ListCell<BaseChatRoomListItem>)?.item to true
        } else {
          //HACKS HACKS HACKS
          //I dunno how to do it otherwise

          //This happens when user selects already selected ListView item
          //We should not reload roomMessageHistory in this case, but at the same time we should show JoinChatRoomDialog
          //so we return false
          findListCellInTree<BaseChatRoomListItem>(event.target as Node)?.item to false
        }

        if (item != null) {
          when (item) {
            is PublicChatRoomItem -> {
              controller.updateSelectedRoom(item.roomName)

              val isMyUserAdded = store.getChatRoomByName(item.roomName)?.isMyUserAdded() ?: false
              if (isMyUserAdded) {
                if (shouldReloadRoomMessageHistory) {
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
      }
    }

    return cell
  }

  private fun createCellNoRoomsNotificationItem(widthProperty: ReadOnlyDoubleProperty): Node {
    return HBox().apply {
      prefHeight = 64.0
      maxHeight = 64.0
      paddingAll = 2.0
      cursor = Cursor.HAND

      maxWidthProperty().bind(widthProperty - rightMargin)

      label("No public chat rooms created yet") { minWidth = 8.0 }
    }
  }

  private fun createCellPublicChatRoomItem(widthProperty: ReadOnlyDoubleProperty, item: PublicChatRoomItem): Node {
    return HBox().apply {
      prefHeight = 64.0
      maxHeight = 64.0
      paddingAll = 2.0
      cursor = Cursor.HAND

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
        label(controller.lastChatMessageMap[item.roomName]!!) {
          textOverrun = OverrunStyle.ELLIPSIS
        }
      }
    }
  }

  private tailrec fun <T> findListCellInTree(node: Node?): ListCell<T>? {
    if (node == null) {
      return null
    }

    if (node is ListCell<*>) {
      return node as ListCell<T>
    }

    if (node is ListView<*>) {
      return null
    }

    return findListCellInTree(node.parent)
  }

  companion object {
    const val componentId = "PublicChatRoomItemCellFragment"
  }
}