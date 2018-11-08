package ui.chat_main_window.create_chat_room_dialog

import ChatApp
import controller.CreateChatRoomDialogController
import core.Status
import events.ChatMainWindowEvents
import events.ChatRoomListFragmentEvents
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import store.settings.SharedSettings
import tornadofx.*
import ui.base.BaseFragment
import utils.UiValidators

class CreateChatRoomDialogFragment : BaseFragment("Create Chat Room") {
  private val controller: CreateChatRoomDialogController by inject()
  private val disableControlsFlag = SimpleBooleanProperty(false)
  private val sharedSettings: SharedSettings by lazy { ChatApp.settingsStore.sharedSettings }

  private var clearSelection = true

  private val model = object : ViewModel() {
    val roomName = bind { SimpleStringProperty(null) }
    val roomPassword = bind { SimpleStringProperty(null) }
    val roomImageUrl = bind { SimpleStringProperty(null) }
    val userName = bind { SimpleStringProperty(sharedSettings.userName) }
    val isPublic = bind { SimpleBooleanProperty(true) }
  }

  override fun onDock() {
    super.onDock()
    controller.createController(this)
  }

  override fun onUndock() {
    if (clearSelection) {
      fire(ChatRoomListFragmentEvents.ClearSelection)
    }

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
            UiValidators.validateRoomName(this, roomName)
          }

          disableWhen { disableControlsFlag }
          whenDocked { requestFocus() }
        }
      }
      field("Room Password") {
        textfield(model.roomPassword) {
          validator { roomPassword ->
            if (roomPassword?.isEmpty() == true) {
              model.roomPassword.value = null
              UiValidators.validateRoomPassword(this, model.roomPassword.value)
            } else {
              UiValidators.validateRoomPassword(this, roomPassword)
            }
          }

          disableWhen { disableControlsFlag }
          tooltip("Create a password protected chat room (May be left empty)")
        }
      }
      field("Room icon image url") {
        textfield(model.roomImageUrl) {
          validator { roomImageUrl ->
            UiValidators.validateRoomImageUrl(this, roomImageUrl)
          }

          disableWhen { disableControlsFlag }
          tooltip("A link to the image that will be used as this room's icon in the global chat room list\n(For now only images from imgur.com are allowed)")
        }
      }
      field("User Name") {
        textfield(model.userName) {
          validator { userName ->
            if (userName?.isEmpty() == true) {
              model.userName.value = null
              UiValidators.validateUserName(this, model.userName.value)
            } else {
              UiValidators.validateUserName(this, userName)
            }
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

  fun closeFragment() {
    doOnUI {
      close()
    }
  }

  fun lockControls() {
    doOnUI {
      disableControlsFlag.set(true)
    }
  }

  fun unlockControls() {
    doOnUI {
      disableControlsFlag.set(false)
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
      fire(ChatRoomListFragmentEvents.ClearSearchInput)
      fire(ChatRoomListFragmentEvents.SelectItem(roomName))

      clearSelection = false
      unlockControls()
      closeFragment()
    }
  }

}