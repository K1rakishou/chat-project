package ui.chat_main_window

import controller.ChatRoomListController
import store.Store
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.input.MouseButton
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import model.PublicChatRoomItem
import tornadofx.Fragment
import tornadofx.hgrow
import tornadofx.listview
import tornadofx.paddingRight

class ChatRoomListFragment : Fragment() {
  val chatRoomListController: ChatRoomListController by inject()
  val store: Store by inject()

  override val root = listview(store.getPublicChatRoomList()) {
    setCellFactory { _ -> cellFactory() }
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
          chatRoomListController.joinChatRoom(item)
        }
      }
    }

    return cell
  }

  private fun createCellItem(item: PublicChatRoomItem): Node {
    return HBox().apply {
      prefHeight = 96.0

      add(Label(item.usersCount.toString()))
      add(Pane().apply {
        paddingRight = 15.0
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