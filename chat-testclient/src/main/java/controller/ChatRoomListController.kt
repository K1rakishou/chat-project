package controller

import ChatApp
import core.ResponseInfo
import core.ResponseType
import core.Status
import core.model.drainable.PublicUserInChat
import core.model.drainable.chat_message.BaseChatMessage
import core.packet.JoinChatRoomPacket
import core.packet.SendChatMessagePacket
import core.response.JoinChatRoomResponsePayload
import core.response.NewChatMessageResponsePayload
import core.response.SendChatMessageResponsePayload
import core.response.UserHasJoinedResponsePayload
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ObservableList
import javafx.scene.control.Alert
import javafx.util.Duration
import kotlinx.coroutines.experimental.launch
import manager.NetworkManager
import model.PublicChatRoomItem
import model.chat_message.BaseChatMessageItem
import model.chat_message.TextChatMessageItem
import tornadofx.Controller
import tornadofx.alert
import tornadofx.runLater
import ui.chat_main_window.ChatRoomView
import ui.chat_main_window.ChatRoomViewEmpty
import java.lang.IllegalStateException

class ChatRoomListController : Controller() {
  private val networkManager = (app as ChatApp).networkManager
  private val store: Store by inject()
  private val keyStore: KeyStore by inject()
  private var selectedRoomName: String? = null

  val scrollToBottomFlag = SimpleBooleanProperty(false)

  init {
    launch { startListeningToPackets() }
  }

  fun getCurrentChatRoomMessageHistory(): ObservableList<BaseChatMessageItem> {
    val roomName = requireNotNull(selectedRoomName)
    return store.getChatRoomMessageHistory(roomName)
  }

  fun sendMessage(messageText: String) {
    if (selectedRoomName == null) {
      println("Cannot send a message since no room is selected")
      return
    }

    if (!store.isUserInRoom(selectedRoomName!!)) {
      println("Cannot send a message since user is not in the room")
      return
    }

    addChatMessage(selectedRoomName!!, TextChatMessageItem(store.getCurrentUserName(), messageText))

    launch {
      networkManager.sendPacket(SendChatMessagePacket(0, selectedRoomName!!, store.getCurrentUserName(), messageText))
    }
  }

  fun joinChatRoom(publicChatRoomItem: PublicChatRoomItem) {
    if (selectedRoomName != null && selectedRoomName == publicChatRoomItem.roomName) {
      return
    }

    if (store.isUserInRoom(publicChatRoomItem.roomName)) {
      //TODO: update current room
      return
    }

    launch {
      selectedRoomName = publicChatRoomItem.roomName
      networkManager.sendPacket(JoinChatRoomPacket(keyStore.getMyPublicKeyEncoded(), store.getCurrentUserName(), publicChatRoomItem.roomName, null))
    }
  }

  private suspend fun startListeningToPackets() {
    for (socketEvent in networkManager.socketEventsQueue.openSubscription()) {
      when (socketEvent) {
        is NetworkManager.SocketEvent.ConnectedToServer -> {
        }
        is NetworkManager.SocketEvent.ErrorWhileConnecting -> {
        }
        is NetworkManager.SocketEvent.DisconnectedFromServer -> {
          onDisconnected()
        }
        is NetworkManager.SocketEvent.ResponseReceived -> {
          handleIncomingResponses(socketEvent.responseInfo)
        }
      }
    }
  }

  private fun handleIncomingResponses(responseInfo: ResponseInfo) {
    when (responseInfo.responseType) {
      ResponseType.JoinChatRoomResponseType -> {
        println("JoinChatRoomResponseType response received")

        val response = JoinChatRoomResponsePayload.fromByteSink(responseInfo.byteSink)
        if (response.status != Status.Ok) {
          showAlert(message = "Error while trying to join a chat room")
          return
        }

        if (selectedRoomName != response.roomName) {
          throw IllegalStateException("The room that the user has selected and the one that the server has sent back do not match up. Wut?")
        }

        val roomName = response.roomName!!
        val users = response.users
        val messageHistory = response.messageHistory

        loadRoomInfo(roomName, users, messageHistory)
      }
      ResponseType.UserHasJoinedResponseType -> {
        println("UserHasJoinedResponseType response received")

        val response = UserHasJoinedResponsePayload.fromByteSink(responseInfo.byteSink)
        if (response.status != Status.Ok) {
          showAlert(message = "UserHasJoinedResponsePayload with non ok status ${response.status}")
          return
        }

        val roomName = response.roomName!!
        addChatMessage(roomName, TextChatMessageItem("Server", "User \"${response.user!!.userName}\" has joined to chat room"))
      }
      ResponseType.SendChatMessageResponseType -> {
        println("SendChatMessageResponseType response received")

        val response = SendChatMessageResponsePayload.fromByteSink(responseInfo.byteSink)
        if (response.status != Status.Ok) {
          showAlert(message = "SendChatMessageResponseType with non ok status ${response.status}")
          return
        }
      }
      ResponseType.NewChatMessageResponseType -> {
        println("NewChatMessageResponseType response received")

        val response = NewChatMessageResponsePayload.fromByteSink(responseInfo.byteSink)
        if (response.status != Status.Ok) {
          showAlert(message =  "NewChatMessageResponsePayload with non ok status ${response.status}")
          return
        }

        addChatMessage(response.roomName!!, TextChatMessageItem(response.userName!!, response.message!!))
      }
      else -> {
        //Do nothing
      }
    }
  }

  private fun addChatMessage(roomName: String, chatMessage: BaseChatMessageItem) {
    runLater {
      store.addChatRoomMessage(roomName, chatMessage)
    }
  }

  private fun loadRoomInfo(roomName: String, users: List<PublicUserInChat>, messageHistory: List<BaseChatMessage>) {
    runLater {
      find<ChatRoomViewEmpty>().replaceWith<ChatRoomView>()

      //Wait some time before ChatRoomView shows up
      runLater(Duration.millis(250.0)) {
        store.addChatRoomMessage(roomName, TextChatMessageItem("Server", "You've joined the chat room"))

        store.addJoinedRoom(roomName)
        store.setChatRoomUserList(roomName, users)
        store.setChatRoomMessageList(roomName, messageHistory)

        scrollToBottomFlag.set(true)
      }
    }
  }

  private fun showAlert(message: String = "", header: String = "", type: Alert.AlertType = Alert.AlertType.INFORMATION) {
    runLater {
      alert(type, header, message)
    }
  }

  private fun onDisconnected() {
    runLater {
      store.clearPublicChatRoomList()
      alert(Alert.AlertType.INFORMATION, "Disconnected from the server")
    }
  }
}