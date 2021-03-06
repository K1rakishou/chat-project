package ui.chat_main_window.join_chat_room_dialog

import ChatApp
import controller.JoinChatRoomDialogController
import core.Status
import core.model.drainable.PublicUserInChat
import core.model.drainable.chat_message.BaseChatMessage
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

class JoinChatRoomDialogFragment : BaseFragment("Join Chat Room") {
  private val disableControlsFlag = SimpleBooleanProperty(false)
  private val roomNameItem: RoomNameItem by inject()
  private val controller: JoinChatRoomDialogController by inject()
  private val sharedSettings: SharedSettings by lazy { ChatApp.settingsStore.sharedSettings }

  private var clearSelection = true

  private val model = object : ViewModel() {
    val userName = bind { SimpleStringProperty(sharedSettings.userName) }
    val roomPassword = bind { SimpleStringProperty(null) }
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
    prefWidth = 280.0
    paddingAll = 10.0

    fieldset {
      field("User Name") {
        textfield(model.userName) {
          validator { userName ->
            UiValidators.validateUserName(this, userName, requireUserName = true)
          }

          whenDocked { requestFocus() }
          disableWhen { disableControlsFlag }
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
        }
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
        disableWhen { disableControlsFlag }

        action {
          model.commit {
            controller.joinChatRoom(roomNameItem.roomName, model.userName.value, model.roomPassword.value)
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

  fun onJoinedToChatRoom(
    roomName: String,
    roomImageUrl: String,
    userName: String,
    users: List<PublicUserInChat>,
    messageHistory: List<BaseChatMessage>
  ) {
    doOnUI {
      fire(ChatMainWindowEvents.JoinedChatRoomEvent(roomName, roomImageUrl, userName, users, messageHistory))
      fire(ChatRoomListFragmentEvents.ClearSearchInput)

      clearSelection = false
      unlockControls()
      closeFragment()
    }
  }

  fun onFailedToJoinChatRoom(status: Status) {
    doOnUI {
      val errorMessage = status.toErrorMessage()

      showErrorAlert(errorMessage)
      unlockControls()
    }
  }

  fun onError(message: String) {
    showErrorAlert(message)
    unlockControls()
  }

  class RoomNameItem(val roomName: String) : ItemViewModel<String>()
}