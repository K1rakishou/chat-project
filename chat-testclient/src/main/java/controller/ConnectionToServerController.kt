package controller

import core.Status
import core.response.GetPageOfPublicRoomsResponsePayload
import kotlinx.coroutines.experimental.launch
import tornadofx.Controller
import ChatApp
import core.packet.GetPageOfPublicRoomsPacketPayload
import core.response.BaseResponse
import javafx.beans.property.SimpleStringProperty
import kotlinx.coroutines.experimental.delay
import manager.NetworkManager
import tornadofx.runLater
import ui.chat_room_list_window.ChatRoomListWindow
import ui.loading_window.ConnectionToServerWindow
import java.util.concurrent.atomic.AtomicInteger

class ConnectionToServerController : Controller() {
  private val networkManager = (app as ChatApp).networkManager
  private var delayTime = AtomicInteger(3000)
  private val maxDelayTime: Int = 15000

  val store: Store by inject()
  val connectionStatus = SimpleStringProperty("")

  init {
    startConnectionToServer()
    launch { startListeningToPackets() }
  }

  fun startConnectionToServer() {
    launch {
      delay(increaseDelayAndGet())

      changeConnectionStatus("Connecting...")
      networkManager.connect()
    }
  }

  private suspend fun startListeningToPackets() {
    for (socketEvent in networkManager.socketEventsQueue.openSubscription()) {
      when (socketEvent) {
        is NetworkManager.SocketEvent.ConnectedToServer -> {
          changeConnectionStatus("Connected")
          networkManager.sendPacket(GetPageOfPublicRoomsPacketPayload(0, 20))
        }
        is NetworkManager.SocketEvent.ErrorWhileConnecting -> {
          changeConnectionStatus("Error while trying to connect: ${socketEvent.throwable.message}")
          startConnectionToServer()
        }
        is NetworkManager.SocketEvent.DisconnectedFromServer -> {
          changeConnectionStatus("Reconnecting...")
          startConnectionToServer()
        }
        is NetworkManager.SocketEvent.PacketReceived -> {
          handleIncomingPackets(socketEvent.packet)
        }
      }
    }
  }

  private fun handleIncomingPackets(response: BaseResponse) {
    when (response) {
      is GetPageOfPublicRoomsResponsePayload -> {
        if (response.status == Status.Ok) {
          runLater {
            store.setChatRoomList(response.publicChatRoomList)
            find<ConnectionToServerWindow>().replaceWith<ChatRoomListWindow>()
          }
        }
      }
    }
  }

  private fun increaseDelayAndGet(): Int {
    if (delayTime.get() < maxDelayTime) {
      return delayTime.incrementAndGet()
    }

    return delayTime.get()
  }

  private fun changeConnectionStatus(content: String?) {
    runLater {
      connectionStatus.set(content ?: "Unknown error")
    }
  }
}