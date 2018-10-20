package ui.chat_main_window

import Styles
import controller.ChatMainWindowController
import javafx.scene.control.ScrollPane
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import kotlinx.coroutines.delay
import model.chat_message.BaseChatMessageItem
import model.chat_message.MyTextChatMessageItem
import tornadofx.*
import ui.base.BaseView
import java.util.concurrent.atomic.AtomicInteger


class ChatRoomView : BaseView() {
  private val delayBeforeUpdatingScrollBarPosition = 50.0
  private val childIndex = AtomicInteger(0)
  private val chatMainWindowController: ChatMainWindowController by inject()

  private lateinit var scrollPane: ScrollPane

  init {
    chatMainWindowController.scrollToBottomFlag.addListener { _, _, _ ->
      scrollToBottom()
    }
  }

  override val root = vbox {
    vboxConstraints { vGrow = Priority.ALWAYS }

    scrollPane = scrollpane {
      hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
      vbarPolicy = ScrollPane.ScrollBarPolicy.ALWAYS
      vboxConstraints { vGrow = Priority.ALWAYS }

      textflow {
        vboxConstraints { vGrow = Priority.ALWAYS }

        bindChildren(chatMainWindowController.currentChatRoomMessageList) { baseChatMessage ->
          return@bindChildren when (baseChatMessage.getMessageType()) {
            BaseChatMessageItem.MessageType.MyTextMessage,
            BaseChatMessageItem.MessageType.ForeignTextMessage,
            BaseChatMessageItem.MessageType.SystemTextMessage -> {
              baseChatMessage as MyTextChatMessageItem
              createTextChatMessage(baseChatMessage.senderName, baseChatMessage.messageText)
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

      setOnAction {
        if (text.isEmpty()) {
          return@setOnAction
        }

        chatMainWindowController.sendMessage(text)

        clear()
        requestFocus()
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

  private fun createTextChatMessage(senderName: String, messageText: String): Text {
    return Text("$senderName: $messageText\n").apply {
      id = "child_id_${childIndex.get()}"
    }
  }
}