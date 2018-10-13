package ui.chat_main_window

import Styles
import controller.ChatMainWindowController
import javafx.scene.control.ScrollPane
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.util.Duration
import model.chat_message.TextChatMessageItem
import store.Store
import tornadofx.*
import java.util.concurrent.atomic.AtomicInteger


class ChatRoomView : View() {
  private val delayBeforeUpdatingScrollBarPosition = 50.0
  private val childIndex = AtomicInteger(0)

  private lateinit var textFlow: TextFlow
  private lateinit var scrollPane: ScrollPane

  private val chatMainWindowController: ChatMainWindowController by inject()
  private val store: Store by inject()

  init {
    chatMainWindowController.scrollToBottomFlag.addListener { _, _, _ ->
      scrollToBottom()
    }
  }

  override val root = vbox {
    scrollPane = scrollpane {
      hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
      vbarPolicy = ScrollPane.ScrollBarPolicy.ALWAYS

      textFlow = textflow {
        bindChildren(chatMainWindowController.getCurrentChatRoomMessageHistory()) { baseChatMessage ->
          return@bindChildren when (baseChatMessage) {
            is TextChatMessageItem -> createTextChatMessage(baseChatMessage.senderName, baseChatMessage.messageText)
            else -> throw IllegalArgumentException("Not implemented for ${baseChatMessage::class}")
          }
        }

        addClass(Styles.chatRoomTextArea)
      }

      vboxConstraints { vGrow = Priority.ALWAYS }
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

  private fun scrollToBottom() {
    //goddamn hacks I swear
    //So, if you don't add a delay here before trying to update scrollbar's position it will scroll to the
    //currentItemPosition-1 and not to the last one because it needs some time to calculate that item's size
    runLater(Duration.millis(delayBeforeUpdatingScrollBarPosition)) {
      scrollPane.vvalue = 1.0
    }
  }

  private fun createTextChatMessage(senderName: String, messageText: String): Text {
    return Text("$senderName: $messageText\n").apply {
      id = "child_id_${childIndex.get()}"
    }
  }
}