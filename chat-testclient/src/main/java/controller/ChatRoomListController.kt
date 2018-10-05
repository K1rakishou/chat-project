package controller

import ChatApp
import core.ResponseInfo
import core.ResponseType
import core.packet.JoinChatRoomPacket
import core.response.JoinChatRoomResponsePayload
import javafx.collections.FXCollections
import javafx.scene.control.Alert
import kotlinx.coroutines.experimental.launch
import manager.NetworkManager
import model.PublicChatRoomItem
import tornadofx.Controller
import tornadofx.alert
import tornadofx.runLater

class ChatRoomListController : Controller() {
  private val networkManager = (app as ChatApp).networkManager
  private val store: Store by inject()
  private val keyStrore: KeyStore by inject()

//  val currentSelectedRoom = SimpleObjectProperty<String>()
  val chatRooms = FXCollections.observableArrayList<PublicChatRoomItem>()

  init {
    chatRooms.addAll(store.getChatRoomList().map { PublicChatRoomItem(it.roomName, it.usersCount) })

    launch { startListeningToPackets() }
  }

  fun joinChatRoom(publicChatRoomItem: PublicChatRoomItem) {
    if (store.isAlreadyJoined(publicChatRoomItem.roomName)) {
      //TODO: update current room
      return
    }

    launch {
      networkManager.sendPacket(JoinChatRoomPacket(keyStrore.getMyPublicKeyEncoded(), "test", publicChatRoomItem.roomName, null))
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

        println()
      }
      ResponseType.UserHasJoinedResponseType -> {
        println("UserHasJoinedResponseType response received")
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