package ui.chat_main_window

import Styles
import controller.ChatRoomListController
import javafx.scene.control.TextArea
import javafx.scene.layout.Priority
import tornadofx.*


class ChatRoomView : View() {
  private val textAreaId = "chatMessagesTextArea"
  private lateinit var textArea: TextArea

  val chatRoomListController: ChatRoomListController by inject()

  override val root = vbox {
    textArea = textarea {
      id = textAreaId

      bind(chatRoomListController.selectedChatRoom.get().roomMessagesProperty())

      addClass(Styles.chatRoomTextArea)
      vboxConstraints { vGrow = Priority.ALWAYS }

      isEditable = false
    }
    textfield {
      addClass(Styles.chatRoomTextField)
      promptText = "Enter your message here"

      setOnAction {
        textArea.appendText("$text\n")

        clear()
      }
    }
  }
}