package ui.chat_main_window

import Styles
import controller.ChatRoomListController
import javafx.scene.control.ScrollPane
import javafx.scene.layout.Priority
import javafx.scene.text.TextFlow
import javafx.util.Duration
import tornadofx.*


class ChatRoomView : View() {
  private val delayBeforeUpdatingScrollBarPosition = 50.0
  private val textAreaId = "chatMessagesTextArea"

  private lateinit var textFlow: TextFlow
  private lateinit var scrollPane: ScrollPane

  val chatRoomListController: ChatRoomListController by inject()

  override val root = vbox {
    scrollPane = scrollpane {
      hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
      vbarPolicy = ScrollPane.ScrollBarPolicy.ALWAYS

      textFlow = textflow {
        id = textAreaId

        addClass(Styles.chatRoomTextArea)
      }

      vboxConstraints { vGrow = Priority.ALWAYS }
    }
    textfield {
      addClass(Styles.chatRoomTextField)
      promptText = "Enter your message here"

      setOnAction {
        val text = if (textFlow.children.size == 0) {
          text(text)
        } else {
          text("\n$text")
        }

        textFlow.children.add(text)

        clear()
        requestFocus()

        //goddamn hacks I swear
        //So, if you don't add a delay here before trying to update scrollbar's position it will scroll to the
        //currentItemPosition-1 and not to the last one because it needs some time to calculate that item's size
        runLater(Duration.millis(delayBeforeUpdatingScrollBarPosition)) {
          scrollPane.vvalue = 1.0
        }
      }
    }
  }
}