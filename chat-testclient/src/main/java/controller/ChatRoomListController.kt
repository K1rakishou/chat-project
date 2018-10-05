package controller

import ChatApp
import core.ResponseInfo
import core.ResponseType
import core.Status
import core.packet.JoinChatRoomPacket
import core.response.JoinChatRoomResponsePayload
import core.response.UserHasJoinedResponsePayload
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.scene.control.Alert
import kotlinx.coroutines.experimental.launch
import manager.NetworkManager
import model.PublicChatRoomItem
import model.PublicUserInChatItem
import tornadofx.Controller
import tornadofx.alert
import tornadofx.runLater
import ui.chat_main_window.ChatRoomView
import ui.chat_main_window.ChatRoomViewEmpty

class ChatRoomListController : Controller() {
  private val networkManager = (app as ChatApp).networkManager
  private val store: Store by inject()
  private val keyStore: KeyStore by inject()

  val chatRooms = FXCollections.observableArrayList<PublicChatRoomItem>()
  val chatRoomUsers = FXCollections.observableArrayList<PublicUserInChatItem>()
  val selectedChatRoom = SimpleObjectProperty<PublicChatRoomItem>()

  init {
    chatRooms.addAll(store.getChatRoomList().map { PublicChatRoomItem(it.roomName, it.usersCount, it.getRoomMessagesAsString()) })

    launch { startListeningToPackets() }
  }

  fun joinChatRoom(publicChatRoomItem: PublicChatRoomItem) {
    if (store.isAlreadyJoined(publicChatRoomItem.roomName)) {
      //TODO: update current room
      return
    }

    launch {
      networkManager.sendPacket(JoinChatRoomPacket(keyStore.getMyPublicKeyEncoded(), "test", publicChatRoomItem.roomName, null))
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

        store.addUserInChatRoomList(response.users)
        chatRoomUsers.addAll(response.users.map { PublicUserInChatItem(it.userName, it.ecPublicKey) })
        selectedChatRoom.value = chatRooms.firstOrNull { it.roomName == response.roomName }

        runLater {
          find<ChatRoomViewEmpty>().replaceWith<ChatRoomView>()
        }
      }
      ResponseType.UserHasJoinedResponseType -> {
        println("UserHasJoinedResponseType response received")

        val response = UserHasJoinedResponsePayload.fromByteSink(responseInfo.byteSink)
        if (response.status != Status.Ok) {
          alert(Alert.AlertType.INFORMATION, "UserHasJoinedResponsePayload with non ok status ${response.status}")
          return
        }

        println()
      }
      else -> {
        //Do nothing
      }
    }
  }

  private fun onDisconnected() {
    runLater {
      alert(Alert.AlertType.INFORMATION, "Disconnected from the server")
      chatRooms.clear()
    }
  }
}