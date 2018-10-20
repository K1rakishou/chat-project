package ui.chat_main_window.create_chat_room_dialog

import controller.CreateChatRoomDialogController
import core.Status
import events.ChatMainWindowEvents
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import tornadofx.*
import ui.base.BaseFragment

class CreateChatRoomDialogFragment : BaseFragment("Create Chat Room") {
  private val controller: CreateChatRoomDialogController by inject()
  private val disableControlsFlag = SimpleBooleanProperty(false)

  private val model = object : ViewModel() {
    val roomName = bind { SimpleStringProperty(null) }
    val roomPassword = bind { SimpleStringProperty(null) }
    val roomImageUrl = bind { SimpleStringProperty(null) }
    val userName = bind { SimpleStringProperty(null) }
    val isPublic = bind { SimpleBooleanProperty(true) }
  }

  override fun onDock() {
    super.onDock()
    controller.createController(this)
  }

  override fun onUndock() {
    controller.destroyController()
    super.onUndock()
  }

  override val root = form {
    prefHeight = 160.0
    prefWidth = 400.0
    paddingAll = 10.0

    fieldset {
      field("Room Name") {
        textfield(model.roomName) {
          required()
          disableWhen { disableControlsFlag }
          whenDocked { requestFocus() }
        }
      }
      field("Room Password") {
        textfield(model.roomPassword) {
          disableWhen { disableControlsFlag }
          tooltip("Create a password protected chat room (May be left empty)")
        }
      }
      field("Room icon image url") {
        textfield(model.roomImageUrl) {
          required()
          disableWhen { disableControlsFlag }
          tooltip("A link to the image that will be used as this room's icon in the global chat room list")
        }
      }
      field("User Name") {
        textfield(model.userName) {
          disableWhen { disableControlsFlag }
          tooltip("Used to automatically joined this room (May be left empty)")
        }
      }
      field {
        checkbox("Is Public", model.isPublic) {
          disableWhen { disableControlsFlag }
          tooltip("Will make this room visible in the global chat room list")
        }
      }
    }
    vbox {
      minHeight = 24.0
    }
    hbox {
      alignment = Pos.BASELINE_RIGHT
      hgrow = Priority.ALWAYS

      button("Create") {
        minWidth = 64.0
        isDefaultButton = true
        disableWhen { disableControlsFlag }

        action {
          model.commit {
            //TODO: add validators (text length, banned symbols, etc)

            controller.createChatRoom(
              model.roomName.value,
              model.roomPassword.value,
              model.roomImageUrl.value,
              model.userName.value,
              model.isPublic.value
            )
          }
        }
      }
      label {
        paddingRight = 12.0
      }
      button("Cancel") {
        minWidth = 64.0

        action {
          closeFragment()
        }
      }
    }
  }

  fun closeFragment() {
    doOnUI {
      close()
    }
  }

  fun lockControls() {
    doOnUI {
      //TODO
    }
  }

  fun unlockControls() {
    doOnUI {
      //TODO
    }
  }

  fun onError(message: String) {
    doOnUI {
      showErrorAlert(message)
      unlockControls()
    }
  }

  fun onFailedToCreateChatRoom(status: Status) {
    doOnUI {
      showErrorAlert(status.toErrorMessage())
      unlockControls()
    }
  }

  fun onChatRoomCreated(roomName: String, roomPassword: String?, roomImageUrl: String, isPublic: Boolean) {
    doOnUI {
      fire(ChatMainWindowEvents.ChatRoomCreatedEvent(roomName, roomImageUrl))
      unlockControls()
      closeFragment()
    }
  }

}