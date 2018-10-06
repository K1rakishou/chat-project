package controller

import ChatApp
import core.ResponseInfo
import core.ResponseType
import core.Status
import core.packet.JoinChatRoomPacket
import core.response.JoinChatRoomResponsePayload
import core.response.UserHasJoinedResponsePayload
import javafx.collections.ObservableList
import javafx.scene.control.Alert
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

  init {
    launch { startListeningToPackets() }
  }

  fun getCurrentChatRoomMessageHistory(): ObservableList<BaseChatMessageItem> {
    val roomName = requireNotNull(selectedRoomName)
    return store.getChatRoomMessageHistory(roomName)
  }

  fun sendMessage(messageText: String) {

  }

  fun joinChatRoom(publicChatRoomItem: PublicChatRoomItem) {
    if (selectedRoomName != null && selectedRoomName == publicChatRoomItem.roomName) {
      return
    }

    if (store.isAlreadyJoined(publicChatRoomItem.roomName)) {
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
          alert(Alert.AlertType.INFORMATION, "Error while trying to join a chat room")
          return
        }

        if (selectedRoomName != response.roomName) {
          throw IllegalStateException("The user has selected and the one that sent server does not match up. Wut?")
        }

        runLater {
          store.setChatRoomUserList(response.roomName!!, response.users)
          store.setChatRoomMessageList(response.roomName!!, response.messageHistory)

          find<ChatRoomViewEmpty>().replaceWith<ChatRoomView>()

          store.addChatRoomMessage(response.roomName!!, TextChatMessageItem("Server", "You've joined the chat room"))
        }
      }
      ResponseType.UserHasJoinedResponseType -> {
        println("UserHasJoinedResponseType response received")

        val response = UserHasJoinedResponsePayload.fromByteSink(responseInfo.byteSink)
        if (response.status != Status.Ok) {
          alert(Alert.AlertType.INFORMATION, "UserHasJoinedResponsePayload with non ok status ${response.status}")
          return
        }

        runLater {
          store.addChatRoomMessage(response.roomName!!, TextChatMessageItem("Server", "User \"${response.roomName!!}\" has joined to chat room"))
        }
      }
      else -> {
        //Do nothing
      }
    }
  }

  private fun onDisconnected() {
    runLater {
      store.clearPublicChatRoomList()
      alert(Alert.AlertType.INFORMATION, "Disconnected from the server")
    }
  }
}