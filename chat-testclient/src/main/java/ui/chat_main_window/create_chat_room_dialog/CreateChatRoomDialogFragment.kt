package ui.chat_main_window.create_chat_room_dialog

import controller.CreateChatRoomDialogController
import core.Constants
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
          validator { roomName ->
            validateRoomName(this, roomName)
          }

          disableWhen { disableControlsFlag }
          whenDocked { requestFocus() }
        }
      }
      field("Room Password") {
        textfield(model.roomPassword) {
          validator { roomPassword ->
            validateRoomPassword(this, roomPassword)
          }

          disableWhen { disableControlsFlag }
          tooltip("Create a password protected chat room (May be left empty)")
        }
      }
      field("Room icon image url") {
        textfield(model.roomImageUrl) {
          validator { roomImageUrl ->
            validateRoomImageUrl(this, roomImageUrl)
          }

          disableWhen { disableControlsFlag }
          tooltip("A link to the image that will be used as this room's icon in the global chat room list (For now only images from imgur.com are allowed)")
        }
      }
      field("User Name") {
        textfield(model.userName) {
          validator { userName ->
            validateUserName(this, userName)
          }

          disableWhen { disableControlsFlag }
          tooltip("Provide user name if you want to automatically join this room (May be left empty)")
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

  private fun validateUserName(context: ValidationContext, userName: String?): ValidationMessage? {
    if (userName == null) {
      return null
    }

    if (userName.isBlank()) {
      return context.error("User name cannot consist solely from whitespaces")
    }

    if (userName.length < Constants.minUserNameLen) {
      return context.error("User name should be at least ${Constants.minUserNameLen} symbols")
    }

    if (userName.length > Constants.maxUserNameLen) {
      return context.error("User name must not exceed ${Constants.maxUserNameLen} symbols")
    }

    return null
  }

  private fun validateRoomImageUrl(context: ValidationContext, roomImageUrl: String?): ValidationMessage? {
    try {
      if (roomImageUrl.isNullOrBlank()) {
        return context.error("Room image url cannot be empty or blank")
      }

      val split1 = roomImageUrl.split("//")
      if (split1[0] != "https:") {
        return context.error("Room image url should start with \"https\"")
      }

      val split2 = split1[1].split("/")
      if (!split2[0].startsWith("i.imgur.com")) {
        return context.error("Not an \"i.imgur.com\" url")
      }

      val split3 = split2[1].split('.')
      if (split3[1] != "jpg" && split3[1] != "png" && split3[1] != "jpeg") {
        return context.error("Image should be either JPG/JPEG or PNG")
      }
    } catch (error: Throwable) {
      return context.error("Parsing error: ${error.message}")
    }

    return null
  }

  private fun validateRoomPassword(context: ValidationContext, roomPassword: String?): ValidationMessage? {
    if (roomPassword == null) {
      return null
    }

    if (roomPassword.isBlank()) {
      return context.error("Room password cannot consist solely from whitespaces")
    }

    if (roomPassword.length < Constants.minChatRoomPasswordLen) {
      return context.error("Room password should be at least ${Constants.minChatRoomPasswordLen} symbols")
    }

    if (roomPassword.length > Constants.maxChatRoomPasswordLen) {
      return context.error("Room password must not exceed ${Constants.maxChatRoomPasswordLen} symbols")
    }

    return null
  }

  private fun validateRoomName(context: ValidationContext, roomName: String?): ValidationMessage? {
    if (roomName.isNullOrBlank()) {
      return context.error("Room name cannot be empty or blank")
    }

    if (roomName.length < Constants.minChatRoomNameLen) {
      return context.error("Room name should be at least ${Constants.minChatRoomNameLen} symbols")
    }

    if (roomName.length > Constants.maxChatRoomNameLength) {
      return context.error("Room name must not exceed ${Constants.maxChatRoomNameLength} symbols")
    }

    return null
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

  fun onChatRoomCreated(roomName: String, roomPassword: String?, roomImageUrl: String, userName: String?, isPublic: Boolean) {
    doOnUI {
      fire(ChatMainWindowEvents.ChatRoomCreatedEvent(roomName, userName, roomImageUrl))
      unlockControls()
      closeFragment()
    }
  }

}