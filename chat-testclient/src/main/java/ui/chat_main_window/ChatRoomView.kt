package ui.chat_main_window

import Styles
import javafx.beans.property.SimpleStringProperty
import javafx.scene.layout.Priority
import tornadofx.*


class ChatRoomView : View() {
  private val model = object : ViewModel() {
    val chatMessages = bind { SimpleStringProperty() }
  }

  override val root = vbox {
    textarea(model.chatMessages) {
      addClass(Styles.chatRoomTextArea)
      vboxConstraints { vGrow = Priority.ALWAYS }
    }
    textfield {
      addClass(Styles.chatRoomTextField)
      promptText = "Enter your message here"

      setOnAction {
        model.chatMessages.value = text
        clear()
      }
    }
  }
}