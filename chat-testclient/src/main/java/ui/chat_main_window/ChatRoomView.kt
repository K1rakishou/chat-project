package ui.chat_main_window

import ChatApp
import Styles
import builders.TransformationBuilder
import controller.ChatMainWindowController
import core.CachingImageLoader
import core.SaveStrategy
import events.ChatRoomViewEvents
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextField
import javafx.scene.layout.*
import javafx.scene.paint.Paint
import kotlinx.coroutines.delay
import model.chat_message.BaseChatMessageItem
import model.chat_message.MessageType
import model.chat_message.text_message.ForeignTextChatMessageItem
import model.chat_message.text_message.MyTextChatMessageItem
import model.chat_message.text_message.SystemChatMessageItem
import model.links.AbstractLink
import org.fxmisc.flowless.VirtualizedScrollPane
import store.ChatRoomsStore
import store.SelectedRoomStore
import tornadofx.*
import ui.base.BaseView
import ui.widgets.VirtualMultiSelectListView


class ChatRoomView : BaseView() {
  private lateinit var messageInput: TextField

  private val chatRoomsStore: ChatRoomsStore by lazy { ChatApp.chatRoomsStore }
  private val selectedRoomStore: SelectedRoomStore by lazy { ChatApp.selectedRoomStore }
  private val imageLoader: CachingImageLoader by lazy { ChatApp.imageLoader }
  private val delayBeforeUpdatingScrollBarPosition = 50.0
  private val scrollbarApproxSize = 16.0
  private val controller: ChatMainWindowController by inject()

  private var currentChatRoomMessagesProperty = FXCollections.observableArrayList<BaseChatMessageItem>()
  private var chatMainWindowSize = params[ChatMainWindow.CHAT_ROOM_VIEW_SIZE] as ChatMainWindow.ChatRoomViewSizeParams

  private val roomMessagePropertyListener = ListConversionListener<BaseChatMessageItem, BaseChatMessageItem>(currentChatRoomMessagesProperty) { it }

  init {
    selectedRoomStore.getSelectedRoomProperty().addListener { _, _, selectedRoomName ->
      reloadMessagesHistory(selectedRoomName)
      messageInputSetFocus()
    }

    subscribe<ChatRoomViewEvents.ScrollToBottom> {
      scrollToBottom()
    }.autoUnsubscribe()
  }

  private val virtualListView = VirtualMultiSelectListView(currentChatRoomMessagesProperty, { baseChatMessage ->
    return@VirtualMultiSelectListView when (baseChatMessage.getMessageType()) {
      MessageType.MyTextMessage -> {
        baseChatMessage as MyTextChatMessageItem
        createMyTextChatMessage(baseChatMessage.senderName, baseChatMessage.messageText, baseChatMessage.isAcceptedByServer(), baseChatMessage.links)
      }
      MessageType.ForeignTextMessage -> {
        baseChatMessage as ForeignTextChatMessageItem
        createForeignTextChatMessage(baseChatMessage.senderName, baseChatMessage.messageText, baseChatMessage.links)
      }
      MessageType.SystemTextMessage -> {
        baseChatMessage as SystemChatMessageItem
        createSystemTextChatMessage(baseChatMessage.senderName, baseChatMessage.messageText)
      }
      MessageType.MyImageMessage -> TODO()
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

    messageInput = textfield {
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

        controller.sendMessage(selectedRoomStore.getSelectedRoom(), text)

        clear()
        requestFocus()
      }
    }
  }

  private fun messageInputSetFocus() {
    messageInput.requestFocus()
  }

  private fun reloadMessagesHistory(selectedRoomName: String?) {
    if (selectedRoomName == null) {
      return
    }

    val prevSelectedChatRoomName = selectedRoomStore.getPrevSelectedRoom()
    val oldRoomMessageProperty = chatRoomsStore.getChatRoomByName(prevSelectedChatRoomName)?.roomMessagesProperty
    val newRoomMessageProperty = chatRoomsStore.getChatRoomByName(selectedRoomName)!!.roomMessagesProperty
    val messageHistory = newRoomMessageProperty ?: emptyList<BaseChatMessageItem>()

    //remove old listener if it's not null
    oldRoomMessageProperty?.removeListener(roomMessagePropertyListener)

    //reload message history for the selected chat room
    currentChatRoomMessagesProperty.clear()
    currentChatRoomMessagesProperty.addAll(messageHistory)

    //add new listener
    newRoomMessageProperty!!.addListener(roomMessagePropertyListener)
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

  //TODO: extract to it's own class?
  private fun createForeignTextChatMessage(
    senderName: String,
    messageText: String,
    links: List<AbstractLink>
  ): Node {
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
        createImageViews(links)
      }
    }
  }

  //TODO: extract to it's own class?
  private fun createMyTextChatMessage(
    senderName: String,
    messageText: String,
    acceptedByServer: Boolean,
    links: List<AbstractLink>
  ): Node {
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
        createImageViews(links)
      }
    }
  }

  private fun Node.createImageViews(links: List<AbstractLink>) {
    if (links.isEmpty()) {
      return
    }

    flowpane {
      orientation = Orientation.HORIZONTAL
      paddingAll = 4.0
      hgap = 4.0
      vgap = 4.0

      for (link in links) {
        vbox {
          imageview {
            fitWidth = 128.0
            fitHeight = 128.0
            isPreserveRatio = true
            isSmooth = true
            cursor = Cursor.HAND

            paddingLeft = 4.0
            paddingRight = 4.0

            imageLoader.newRequest()
              .load(link.value)
              .transformations(
                TransformationBuilder()
                  .centerCrop(this)
              )
              .saveStrategy(SaveStrategy.SaveOriginalImage)
              .into(this)
          }
        }
      }
    }
  }

  //TODO: extract to it's own class?
  private fun createSystemTextChatMessage(
    senderName: String,
    messageText: String
  ): Node {
    return hbox {
      paddingTop = 2.0
      paddingBottom = 2.0
      paddingLeft = 4.0
      paddingRight = 4.0

      prefWidthProperty().bind(chatMainWindowSize.widthProperty - scrollbarApproxSize)

      vbox {
        label(senderName) {
          addClass(Styles.systemMessage)
        }
        label(messageText) {
          //TODO: text wrapping does not work with the "Label" control
          prefWidthProperty().bind(chatMainWindowSize.widthProperty - scrollbarApproxSize)

          addClass(Styles.systemTextChatMessage)
        }
      }
    }
  }
}