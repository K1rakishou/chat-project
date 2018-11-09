package ui.chat_main_window

import ChatApp
import Styles
import controller.ChatMainWindowController
import events.ChatRoomViewEvents
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.ScrollPane
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.paint.Paint
import kotlinx.coroutines.delay
import model.chat_message.BaseChatMessageItem
import model.chat_message.MessageType
import model.chat_message.text_message.ForeignTextChatMessageItem
import model.chat_message.text_message.MyTextChatMessageItem
import model.chat_message.text_message.SystemChatMessageItem
import org.fxmisc.flowless.VirtualizedScrollPane
import store.ChatRoomsStore
import tornadofx.*
import ui.base.BaseView
import ui.widgets.VirtualMultiSelectListView
import java.util.concurrent.atomic.AtomicInteger


class ChatRoomView : BaseView() {
  private val chatRoomsStore: ChatRoomsStore by lazy { ChatApp.chatRoomsStore }
  private val delayBeforeUpdatingScrollBarPosition = 50.0
  private val scrollbarApproxSize = 16.0
  private val controller: ChatMainWindowController by inject()

  private var currentChatRoomMessagesProperty = FXCollections.observableArrayList<BaseChatMessageItem>()
  private var chatMainWindowSize = params[ChatMainWindow.CHAT_ROOM_VIEW_SIZE] as ChatMainWindow.ChatRoomViewSizeParams

  private val roomMessagePropertyListener = ListConversionListener<BaseChatMessageItem, BaseChatMessageItem>(currentChatRoomMessagesProperty) { it }

  init {
    chatRoomsStore.selectedRoomStore.getSelectedRoom().addListener { _, _, selectedRoomName ->
      reloadMessagesHistory(selectedRoomName)
    }

    controller.scrollToBottomFlag.addListener { _, _, _ ->
      scrollToBottom()
    }
  }

  private val virtualListView = VirtualMultiSelectListView(currentChatRoomMessagesProperty, { baseChatMessage ->
    return@VirtualMultiSelectListView when (baseChatMessage.getMessageType()) {
      MessageType.MyTextMessage -> {
        baseChatMessage as MyTextChatMessageItem
        createMyTextChatMessage(baseChatMessage.senderName, baseChatMessage.messageText, baseChatMessage.isAcceptedByServer())
      }
      MessageType.ForeignTextMessage -> {
        baseChatMessage as ForeignTextChatMessageItem
        createForeignTextChatMessage(baseChatMessage.senderName, baseChatMessage.messageText)
      }
      MessageType.SystemTextMessage -> {
        baseChatMessage as SystemChatMessageItem
        createSystemTextChatMessage(baseChatMessage.senderName, baseChatMessage.messageText)
      }
      MessageType.MyImageMessage -> TODO()
      else -> throw IllegalArgumentException("Not implemented for ${baseChatMessage::class}")
    }
  })

  //TODO: add margins somehow
  private val virtualScrollPane = VirtualizedScrollPane(virtualListView.getVirtualFlow().apply {
    prefWidthProperty().bind(chatMainWindowSize.widthProperty - scrollbarApproxSize)
    prefHeightProperty().bind(chatMainWindowSize.heightProperty)

    setOnMouseClicked { event ->
      requestFocus()
      virtualListView.onMouseClick(event)
    }
  }).apply {
    hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
    vbarPolicy = ScrollPane.ScrollBarPolicy.ALWAYS

    background = Background(BackgroundFill(Paint.valueOf("#ffffff"), CornerRadii.EMPTY, Insets.EMPTY))
  }

  override val root = vbox {
    handleDragAndDrop(this)

    add(virtualScrollPane)

    textfield {
      addClass(Styles.chatRoomTextField)
      promptText = "Enter your message here"
      whenDocked { requestFocus() }

      focusedProperty().addListener { _, _, focused ->
        if (focused) {
          virtualListView.clearSelection()
        }
      }

      setOnAction {
        if (text.isEmpty()) {
          return@setOnAction
        }

        controller.sendMessage(chatRoomsStore.selectedRoomStore.getSelectedRoom().get(), text)

        clear()
        requestFocus()
      }
    }
  }

  private fun reloadMessagesHistory(selectedRoomName: String?) {
    if (selectedRoomName == null) {
      return
    }

    val oldSelectedChatRoomName = chatRoomsStore.selectedRoomStore.getSelectedRoom().get()
    val oldRoomMessageProperty = chatRoomsStore.getChatRoomByName(oldSelectedChatRoomName)?.roomMessagesProperty
    val newRoomMessageProperty = chatRoomsStore.getChatRoomByName(selectedRoomName)!!.roomMessagesProperty
    val messageHistory = newRoomMessageProperty ?: emptyList<BaseChatMessageItem>()

    //remove old listener if it's not null
    oldRoomMessageProperty?.removeListener(roomMessagePropertyListener)

    //reload message history for the selected chat room
    currentChatRoomMessagesProperty.clear()
    currentChatRoomMessagesProperty.addAll(messageHistory)

    //add new listener
    newRoomMessageProperty!!.addListener(roomMessagePropertyListener)

    chatRoomsStore.selectedRoomStore.setSelectedRoom(selectedRoomName)
  }

  private fun handleDragAndDrop(node: Node) {
    //TODO
//    node.setOnDragOver { event ->
//      if (event.dragboard.hasFiles()) {
//        event.acceptTransferModes(*TransferMode.ANY)
//      }
//    }
//
//    node.setOnDragDropped { event ->
//      event.dragboard.files.forEach {
//        println("file = ${it.absolutePath}")
//
//        controller.addChatMessage(selectedChatRoomName, MyImageChatMessage("test", it))
//      }
//    }
  }

  fun scrollToBottom() {
    //goddamn hacks I swear
    //So, if you don't add a delay here before trying to update scrollbar's position it will scroll to the
    //currentItemPosition-1 and not to the last one because it needs some time to calculate that item's size
    doOnUI {
      delay(delayBeforeUpdatingScrollBarPosition.toLong())

      virtualScrollPane.content.showAsLast(virtualListView.getLastItemIndex())
    }
  }

  //TODO
//  private fun createImageChatMessage(senderName: String, imageFile: File): Node {
//    return VBox().apply {
//      id = "child_id_${childIndex.get()}"
//      cursor = Cursor.HAND
//
//      text(senderName)
//
//      val image = imageFile.inputStream().use { stream -> Image(stream) }
//      imageview(image) {
//        fitWidth = 128.0
//        fitHeight = 128.0
//      }
//    }
//  }

  private fun createForeignTextChatMessage(senderName: String, messageText: String): Node {
    return hbox {
      paddingTop = 2.0
      paddingBottom = 2.0
      paddingLeft = 4.0
      paddingRight = 4.0

      prefWidthProperty().bind(chatMainWindowSize.widthProperty - scrollbarApproxSize)

      vbox {
        label(senderName) {
          addClass(Styles.receiverName)
        }
        label(messageText) {
          //TODO: text wrapping does not work with the "Label" control
          prefWidthProperty().bind(chatMainWindowSize.widthProperty - scrollbarApproxSize)

          addClass(Styles.foreignTextChatMessage)
        }
      }
    }
  }

  private fun createSystemTextChatMessage(senderName: String, messageText: String): Node {
    return hbox {
      paddingTop = 2.0
      paddingBottom = 2.0
      paddingLeft = 4.0
      paddingRight = 4.0

      prefWidthProperty().bind(chatMainWindowSize.widthProperty - scrollbarApproxSize)

      vbox {
        label(senderName) {
          addClass(Styles.senderName)
        }
        label(messageText) {
          //TODO: text wrapping does not work with the "Label" control
          prefWidthProperty().bind(chatMainWindowSize.widthProperty - scrollbarApproxSize)

          addClass(Styles.systemTextChatMessage)
        }
      }
    }
  }

  private fun createMyTextChatMessage(senderName: String, messageText: String, acceptedByServer: Boolean): Node {
    return hbox {
      paddingTop = 2.0
      paddingBottom = 2.0
      paddingLeft = 4.0
      paddingRight = 4.0

      prefWidthProperty().bind(chatMainWindowSize.widthProperty - scrollbarApproxSize)

      vbox {
        label(senderName) {
          addClass(Styles.senderName)
        }
        label(messageText) {
          //TODO: text wrapping does not work with the "Label" control
          prefWidthProperty().bind(chatMainWindowSize.widthProperty - scrollbarApproxSize)

          if (!acceptedByServer) {
            addClass(Styles.myTextChatMessage)
          } else {
            addClass(Styles.myTextChatMessageAcceptedByServer)
          }
        }
      }
    }
  }
}