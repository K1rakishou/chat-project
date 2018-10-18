package ui.chat_main_window

import controller.ChatMainWindowController
import events.ChatMainWindowEvents
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.image.ImageView
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

  override val root = listview(controller.publicChatRoomList) {
    vboxConstraints { vGrow = Priority.ALWAYS }
    setCellFactory { cellFactory() }

    chatRoomListView = this
  }

  private fun cellFactory(): ListCell<PublicChatRoomItem> {
    val cell = object : ListCell<PublicChatRoomItem>() {
      override fun updateItem(item: PublicChatRoomItem?, empty: Boolean) {
        super.updateItem(item, empty)

        if (item == null) {
          graphic = null
          return
        }

        //TODO: Optimize.
        //This method is getting called every time a ListView item is clicked.
        graphic = createCellItem(item)
      }
    }

    cell.id = componentId

    cell.setOnMouseClicked { event ->
      if (cell.isEmpty) {
        event.consume()
        return@setOnMouseClicked
      }

      val target = event.target as? Parent
        ?: return@setOnMouseClicked

      if (event.button == MouseButton.PRIMARY && event.clickCount == 1) {
        //TODO: This hack does not work if ListView item's imageView or Label is clicked
        val (item, shouldReloadRoomMessageHistory) = if (target.id == componentId) {
          //This happens normally when not selected ListView item gets selected by user
          (target as? ListCell<PublicChatRoomItem>)?.item to true
        } else {
          //HACKS HACKS HACKS
          //I dunno how to do it otherwise

          //This happens when user selects already selected ListView item
          //We should not reload roomMessageHistory in this case, but at the same time we should show JoinChatRoomDialog
          //so we return false
          ((event.target as? HBox)?.parent as? ListCell<PublicChatRoomItem>)?.item to false
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

  private fun createCellItem(item: PublicChatRoomItem): Node {
    return HBox().apply {
      prefHeight = 64.0
      maxHeight = 64.0
      paddingAll = 4.0
      cursor = Cursor.HAND

      add(ImageView(item.imageUrl).apply {
        fitHeight = 48.0
        isPreserveRatio = true
        isSmooth = true
        alignment = Pos.CENTER_LEFT
        paddingRight = 8.0
      })

      add(Label(item.roomName).apply {
        hgrow = Priority.ALWAYS
      })
    }
  }

  companion object {
    const val componentId = "PublicChatRoomItemCellFragment"
  }
}