package ui.chat_main_window.join_chat_room_dialog

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
import tornadofx.*
import ui.base.BaseFragment

class JoinChatRoomDialogFragment : BaseFragment("Join Chat Room") {
  private val disableControlsFlag = SimpleBooleanProperty(false)
  private var shouldClearSelectedChatRoom = false

  private val roomNameItem: RoomNameItem by inject()
  private val controller: JoinChatRoomDialogController by inject()

  private val model = object : ViewModel() {
    val userName = bind { SimpleStringProperty(null) }
    val roomPassword = bind { SimpleStringProperty(null) }
  }

  override fun onDock() {
    super.onDock()
    controller.createController(this)
  }

  override fun onUndock() {
    controller.destroyController()

    if (shouldClearSelectedChatRoom) {
      fire(ChatRoomListFragmentEvents.ClearRoomSelectionEvent)
    }

    super.onUndock()
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
          disableWhen { disableControlsFlag }
        }
      }
      field("Room Password") {
        textfield(model.roomPassword) {
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
          shouldClearSelectedChatRoom = true
          closeFragment()
        }
      }
    }
  }

  fun closeFragment() {
    runLater {
      close()
    }
  }

  fun lockControls() {
    disableControlsFlag.set(true)
  }

  fun unlockControls() {
    disableControlsFlag.set(false)
  }

  fun onJoinedToChatRoom(
    roomName: String,
    userName: String,
    users: List<PublicUserInChat>,
    messageHistory: List<BaseChatMessage>
  ) {
    shouldClearSelectedChatRoom = false

    fire(ChatMainWindowEvents.JoinedChatRoomEvent(roomName, userName, users, messageHistory))
    unlockControls()

    closeFragment()
  }

  fun onFailedToJoinChatRoom(status: Status) {
    shouldClearSelectedChatRoom = true
    val errorMessage = status.toErrorMessage()

    showErrorAlert(errorMessage)
    unlockControls()
  }

  fun onError(message: String) {
    shouldClearSelectedChatRoom = true
    showErrorAlert(message)
    unlockControls()
  }

  class RoomNameItem(val roomName: String) : ItemViewModel<String>()
}