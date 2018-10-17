package ui.chat_main_window

import controller.ChatMainWindowController
import events.ChatRoomListClearRoomSelectionEvent
import events.ShowJoinChatRoomDialogEvent
import javafx.geometry.Pos
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

class ChatRoomListFragment : Fragment() {
  private val store: Store by inject()
  private val chatMainWindowController: ChatMainWindowController by inject()

  private var chatRoomListView: ListView<PublicChatRoomItem>? = null

  init {
    subscribe<ChatRoomListClearRoomSelectionEvent> {
      runLater {
        chatRoomListView?.selectionModel?.clearSelection()
      }
    }
  }

  override val root = listview(chatMainWindowController.publicChatRoomList) {
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

      if (event.button == MouseButton.PRIMARY && event.clickCount == 1 && target.id == componentId) {
        val item = (target as? ListCell<PublicChatRoomItem>)?.item
        if (item != null) {
          if (store.isUserInRoom(item.roomName)) {
            //if user has not yet joined the room - send join packet
            chatMainWindowController.joinChatRoom(item.roomName)
          } else {
            //otherwise - open a JoinToChatRoom window
            fire(ShowJoinChatRoomDialogEvent(item.roomName))
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