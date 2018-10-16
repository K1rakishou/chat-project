package ui.chat_main_window.join_chat_room_dialog

import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import tornadofx.*

class JoinChatRoomDialogFragment : Fragment("Join Chat Room") {

  private val model = object : ViewModel() {
    val userName = bind { SimpleStringProperty(null) }
    val roomPassword = bind { SimpleStringProperty(null) }
  }

  override val root = form {
    prefHeight = 160.0
    prefWidth = 280.0
    paddingAll = 10.0

    fieldset {
      field("Username") {
        textfield(model.userName) {
          required()
          whenDocked { requestFocus() }
        }
      }
      field("Room Password") {
        textfield(model.roomPassword)
      }
    }

    vbox {
      minHeight = 24.0
    }

    hbox {
      alignment = Pos.BASELINE_RIGHT
      hgrow = Priority.ALWAYS

      button("Join") {
        minWidth = 64.0
        isDefaultButton = true
        enableWhen { model.valid }

        action {
          println("Join")
        }
      }
      label {
        paddingRight = 12.0
      }
      button("Cancel") {
        minWidth = 64.0

        action {
          close()
        }
      }
    }
  }
}