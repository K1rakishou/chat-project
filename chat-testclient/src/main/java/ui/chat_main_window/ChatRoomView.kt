package ui.chat_main_window

import Styles
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.TextArea
import javafx.scene.layout.Priority
import tornadofx.*


class ChatRoomView : View() {
  private lateinit var textArea: TextArea

  private val model = object : ViewModel() {
    val chatMessages = bind { SimpleStringProperty() }
  }

  override val root = vbox {
    textarea(model.chatMessages) {
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

    //this is probably a fucking hack, but I have no idea
    //how to access textArea from textField otherwise ¯\_(ツ)_/¯
    textArea = getChildList()
      ?.first { it is TextArea } as TextArea
  }
}