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
import model.PublicChatRoomItem
import store.Store
import tornadofx.*
import ui.base.BaseFragment

class ChatRoomListFragment : BaseFragment() {
  private val store: Store by inject()
  private val controller: ChatMainWindowController by inject()
  private var chatRoomListView: ListView<PublicChatRoomItem>? = null

  init {
    subscribe<ChatRoomListFragmentEvents.SelectListViewItem> { event ->
      chatRoomListView?.selectionModel?.select(event.itemIndex)
    }.autoUnsubscribe()
  }

  override val root = listview(controller.publicChatRoomList) {
    vboxConstraints { vGrow = Priority.ALWAYS }

    setCellFactory { cellFactory(widthProperty()) }
    chatRoomListView = this
  }

  private fun cellFactory(widthProperty: ReadOnlyDoubleProperty): ListCell<PublicChatRoomItem> {
    val cell = object : ListCell<PublicChatRoomItem>() {
      override fun updateItem(item: PublicChatRoomItem?, empty: Boolean) {
        super.updateItem(item, empty)

        if (item == null) {
          graphic = null
          return
        }

        //TODO: Optimize.
        //This method is getting called every time a ListView item is clicked.
        graphic = createCellItem(widthProperty, item)
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
          (target as? ListCell<PublicChatRoomItem>)?.item to true
        } else {
          //HACKS HACKS HACKS
          //I dunno how to do it otherwise

          //This happens when user selects already selected ListView item
          //We should not reload roomMessageHistory in this case, but at the same time we should show JoinChatRoomDialog
          //so we return false
          findListCellInTree<PublicChatRoomItem>(event.target as Node)?.item to false
        }

        if (item != null) {
          controller.updateSelectedRoom(item.roomName)

          if (store.isUserInRoom(item.roomName)) {
            if (shouldReloadRoomMessageHistory) {
              controller.reloadRoomMessageHistory(item.roomName)
            }
          } else {
            fire(ChatMainWindowEvents.ShowJoinChatRoomDialogEvent(item.roomName))
          }
        }
      }
    }

    return cell
  }

  private fun createCellItem(widthProperty: ReadOnlyDoubleProperty, item: PublicChatRoomItem): Node {
    return HBox().apply {
      prefHeight = 64.0
      maxHeight = 64.0
      paddingAll = 2.0
      cursor = Cursor.HAND

      maxWidthProperty().bind(widthProperty.subtract(16.0))

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