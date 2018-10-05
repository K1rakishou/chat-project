package ui.chat_main_window

import Styles
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.TextArea
import javafx.scene.layout.Priority
import tornadofx.*


class ChatRoomFragment : Fragment() {
  private val textAreaId = "chatMessagesTextArea"
  private lateinit var textArea: TextArea

  private val model = object : ViewModel() {
    val chatMessages = bind { SimpleStringProperty() }
  }

  override val root = vbox {
    textArea = textarea(model.chatMessages) {
      id = textAreaId

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