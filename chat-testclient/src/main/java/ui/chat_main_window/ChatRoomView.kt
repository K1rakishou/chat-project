package ui.chat_main_window

import ChatApp
import Styles
import controller.ChatMainWindowController
import events.ChatRoomViewEvents
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.image.Image
import javafx.scene.input.KeyEvent
import javafx.scene.input.TransferMode
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.VBox
import javafx.scene.paint.Paint
import kotlinx.coroutines.delay
import model.chat_message.BaseChatMessageItem
import model.chat_message.MyImageChatMessage
import model.chat_message.MyTextChatMessageItem
import org.fxmisc.flowless.VirtualizedScrollPane
import store.ChatRoomsStore
import tornadofx.*
import ui.base.BaseView
import ui.widgets.VirtualMultiSelectListView
import java.io.File
import java.util.concurrent.atomic.AtomicInteger


class ChatRoomView : BaseView() {
  private val chatRoomsStore: ChatRoomsStore by lazy { ChatApp.chatRoomsStore }
  private val delayBeforeUpdatingScrollBarPosition = 50.0
  private val scrollbarApproxSize = 16.0
  private val childIndex = AtomicInteger(0)
  private val controller: ChatMainWindowController by inject()

  private var selectedChatRoomName: String = ""
  private var currentChatRoomMessagesProperty = FXCollections.observableArrayList<BaseChatMessageItem>()
  private var chatMainWindowSize = params[ChatMainWindow.CHAT_ROOM_VIEW_SIZE] as ChatMainWindow.ChatRoomViewSizeParams

  private val roomMessagePropertyListener = ListConversionListener<BaseChatMessageItem, BaseChatMessageItem>(currentChatRoomMessagesProperty) { it }

  private val testItems = FXCollections.observableArrayList<BaseChatMessageItem>()

  init {
    testItems.add(MyTextChatMessageItem("test", "1"))
    testItems.add(MyTextChatMessageItem("test", "2"))
    testItems.add(MyTextChatMessageItem("test", "3"))
    testItems.add(MyTextChatMessageItem("test", "4"))
    testItems.add(MyTextChatMessageItem("test", "5"))
    testItems.add(MyTextChatMessageItem("test", "6"))

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

  private val virtualListView = VirtualMultiSelectListView(testItems) { baseChatMessage ->
    baseChatMessage as MyTextChatMessageItem
    createTextChatMessage(baseChatMessage.senderName, baseChatMessage.messageText)
  }

  override val root = vbox {
    addEventFilter(KeyEvent.KEY_PRESSED) { event ->
      virtualListView.updateKeyState(event.isShiftDown, event.isControlDown)
    }
    addEventFilter(KeyEvent.KEY_RELEASED) { event ->
      virtualListView.updateKeyState(event.isShiftDown, event.isControlDown)
    }

    handleDragAndDrop(this)

    add(VirtualizedScrollPane(virtualListView.getVirtualFlow().apply {
      paddingLeft = 10.0

      prefWidthProperty().bind(chatMainWindowSize.widthProperty - scrollbarApproxSize)
      prefHeightProperty().bind(chatMainWindowSize.heightProperty)

      background = Background(BackgroundFill(Paint.valueOf("#ffffff"), CornerRadii.EMPTY, Insets.EMPTY))
    }))
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
//      scrollPane.vvalue = 1.0
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
    return hbox {
      label("$senderName: $messageText\n").apply {
        id = "child_id_${childIndex.get()}"
      }
    }
  }
}