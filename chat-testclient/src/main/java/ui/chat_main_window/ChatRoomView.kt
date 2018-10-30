package ui.chat_main_window

import Styles
import controller.ChatMainWindowController
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.control.ScrollPane
import javafx.scene.image.Image
import javafx.scene.input.TransferMode
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import kotlinx.coroutines.delay
import model.chat_message.MessageType
import model.chat_message.MyImageChatMessage
import model.chat_message.MyTextChatMessageItem
import tornadofx.*
import ui.base.BaseView
import java.io.File
import java.util.concurrent.atomic.AtomicInteger


class ChatRoomView : BaseView() {
  private val delayBeforeUpdatingScrollBarPosition = 50.0
  private val scrollbarApproxSize = 16.0
  private val childIndex = AtomicInteger(0)

  private val controller: ChatMainWindowController by inject()

  private lateinit var scrollPane: ScrollPane

  private val chatMainWindowSize = ChatMainWindow.ChatRoomListFragmentParams(
    params[ChatMainWindow.WIDTH_PROPERTY] as ReadOnlyDoubleProperty,
    params[ChatMainWindow.HEIGHT_PROPERTY] as ReadOnlyDoubleProperty
  )

  init {
    controller.scrollToBottomFlag.addListener { _, _, _ ->
      scrollToBottom()
    }
  }

  override val root = vbox {
    handleDragAndDrop(this)

    scrollPane = scrollpane {
      hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
      vbarPolicy = ScrollPane.ScrollBarPolicy.ALWAYS

      prefHeightProperty().bind(chatMainWindowSize.heightProperty)

      textflow {
        paddingLeft = 10.0
        prefWidthProperty().bind(chatMainWindowSize.widthProperty - scrollbarApproxSize)
        vgrow = Priority.ALWAYS

        bindChildren(controller.currentChatRoomMessageList) { baseChatMessage ->
          return@bindChildren when (baseChatMessage.getMessageType()) {
            MessageType.MyTextMessage,
            MessageType.ForeignTextMessage,
            MessageType.SystemTextMessage -> {
              baseChatMessage as MyTextChatMessageItem
              createTextChatMessage(baseChatMessage.senderName, baseChatMessage.messageText)
            }
            MessageType.MyImageMessage -> {
              baseChatMessage as MyImageChatMessage
              createImageChatMessage(baseChatMessage.senderName, baseChatMessage.imageFile)
            }
            else -> throw IllegalArgumentException("Not implemented for ${baseChatMessage::class}")
          }
        }

        addClass(Styles.chatRoomTextArea)
      }
    }
    textfield {
      addClass(Styles.chatRoomTextField)
      promptText = "Enter your message here"
      whenDocked { requestFocus() }

      setOnAction {
        if (text.isEmpty()) {
          return@setOnAction
        }

        controller.sendMessage(text)

        clear()
        requestFocus()
      }
    }
  }


  private fun handleDragAndDrop(node: Node) {
    node.setOnDragOver { event ->
      if (event.dragboard.hasFiles()) {
        event.acceptTransferModes(*TransferMode.ANY)
      }
    }

    node.setOnDragDropped { event ->
      event.dragboard.files.forEach {
        println("file = ${it.absolutePath}")

        controller.addChatMessage(MyImageChatMessage("test", it))
      }
    }

  }

  fun scrollToBottom() {
    //goddamn hacks I swear
    //So, if you don't add a delay here before trying to update scrollbar's position it will scroll to the
    //currentItemPosition-1 and not to the last one because it needs some time to calculate that item's size
    doOnUI {
      delay(delayBeforeUpdatingScrollBarPosition.toLong())
      scrollPane.vvalue = 1.0
    }
  }

  private fun createImageChatMessage(senderName: String, imageFile: File): Node {
    return VBox().apply {
      id = "child_id_${childIndex.get()}"
      cursor = Cursor.HAND

      text(senderName)

      val image = imageFile.inputStream().use { stream -> Image(stream) }
      imageview(image) {
        fitWidth = 128.0
        fitHeight = 128.0
      }
    }
  }

  private fun createTextChatMessage(senderName: String, messageText: String): Node {
    return Text("$senderName: $messageText\n").apply {
      id = "child_id_${childIndex.get()}"
    }
  }
}