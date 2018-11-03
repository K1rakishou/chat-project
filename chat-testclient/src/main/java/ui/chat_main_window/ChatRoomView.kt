package ui.chat_main_window

import Styles
import controller.ChatMainWindowController
import events.ChatRoomViewEvents
import javafx.collections.FXCollections
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.control.ScrollPane
import javafx.scene.image.Image
import javafx.scene.input.TransferMode
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import kotlinx.coroutines.delay
import model.chat_message.BaseChatMessageItem
import model.chat_message.MessageType
import model.chat_message.MyImageChatMessage
import model.chat_message.MyTextChatMessageItem
import store.ChatRoomsStore
import tornadofx.*
import ui.base.BaseView
import java.io.File
import java.util.concurrent.atomic.AtomicInteger


class ChatRoomView : BaseView() {
  private lateinit var scrollPane: ScrollPane

  private val chatRoomsStore: ChatRoomsStore by lazy { ChatApp.chatRoomsStore }
  private val delayBeforeUpdatingScrollBarPosition = 50.0
  private val scrollbarApproxSize = 16.0
  private val childIndex = AtomicInteger(0)
  private val controller: ChatMainWindowController by inject()

  private var selectedChatRoomName: String = ""
  private var currentChatRoomMessagesProperty = FXCollections.observableArrayList<BaseChatMessageItem>()
  private var chatMainWindowSize = params[ChatMainWindow.CHAT_ROOM_VIEW_SIZE] as ChatMainWindow.ChatRoomViewSizeParams

  private val roomMessagePropertyListener = ListConversionListener<BaseChatMessageItem, BaseChatMessageItem>(currentChatRoomMessagesProperty) { it }

  init {
    subscribe<ChatRoomViewEvents.ChangeSelectedRoom> { event ->
      if (event.selectedRoomName == selectedChatRoomName) {
        return@subscribe
      }

      val oldSelectedChatRoomName = selectedChatRoomName
      selectedChatRoomName = event.selectedRoomName

      val oldRoomMessageProperty = chatRoomsStore.getChatRoomByName(oldSelectedChatRoomName)?.roomMessagesProperty
      val newRoomMessageProperty = chatRoomsStore.getChatRoomByName(selectedChatRoomName)!!.roomMessagesProperty
      val messageHistory = newRoomMessageProperty ?: emptyList<BaseChatMessageItem>()

      //remove old listener if it's not null
      oldRoomMessageProperty?.removeListener(roomMessagePropertyListener)

      //reload message history for the selected chat room
      currentChatRoomMessagesProperty.clear()
      currentChatRoomMessagesProperty.addAll(messageHistory)

      //add new listener
      newRoomMessageProperty!!.addListener(roomMessagePropertyListener)
    }

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

      //TODO: remove TextFlow
      textflow {
        paddingLeft = 10.0
        prefWidthProperty().bind(chatMainWindowSize.widthProperty - scrollbarApproxSize)
        vgrow = Priority.ALWAYS

        bindChildren(currentChatRoomMessagesProperty) { baseChatMessage ->
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

        controller.sendMessage(selectedChatRoomName, text)

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

        //TODO
        controller.addChatMessage(selectedChatRoomName, MyImageChatMessage("test", it))
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