package controller

import ChatApp
import core.ResponseInfo
import core.ResponseType
import core.Status
import core.exception.ResponseDeserializationException
import core.packet.GetPageOfPublicRoomsPacket
import core.response.GetPageOfPublicRoomsResponsePayload
import javafx.beans.property.SimpleStringProperty
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import manager.NetworkManager
import store.Store
import tornadofx.runLater
import ui.chat_main_window.ChatMainWindow
import ui.loading_window.ConnectionToServerWindow
import java.util.concurrent.atomic.AtomicInteger

class ConnectionToServerController : BaseController() {
  private val networkManager = (app as ChatApp).networkManager
  private var delayTime = AtomicInteger(3000)
  private val maxDelayTime: Int = 15000

  val store: Store by inject()
  val connectionStatus = SimpleStringProperty("")

  init {
    startConnectionToServer(true)
    launch { startListeningToPackets() }
  }

  fun startConnectionToServer(firstTime: Boolean = false) {
    launch {
      if (!firstTime) {
        delay(increaseDelayAndGet())
      }

      changeConnectionStatus("Connecting...")
      networkManager.connect()
    }
  }

  private suspend fun startListeningToPackets() {
    for (socketEvent in networkManager.socketEventsQueue.openSubscription()) {
      when (socketEvent) {
        is NetworkManager.SocketEvent.ConnectedToServer -> {
          changeConnectionStatus("Connected")
          networkManager.sendPacket(GetPageOfPublicRoomsPacket(0, 20))
        }
        is NetworkManager.SocketEvent.ErrorWhileConnecting -> {
          changeConnectionStatus("Error while trying to connect: ${socketEvent.throwable.message}")
          startConnectionToServer()
        }
        is NetworkManager.SocketEvent.DisconnectedFromServer -> {
          changeConnectionStatus("Reconnecting...")
          startConnectionToServer()
        }
        is NetworkManager.SocketEvent.ResponseReceived -> {
          handleIncomingResponses(socketEvent.responseInfo)
        }
      }
    }
  }

  private fun handleIncomingResponses(responseInfo: ResponseInfo) {
    when (responseInfo.responseType) {
      ResponseType.GetPageOfPublicRoomsResponseType -> {
        val response = try {
          GetPageOfPublicRoomsResponsePayload.fromByteSink(responseInfo.byteSink)
        } catch (error: ResponseDeserializationException) {
          showErrorAlert("Could not deserialize packet GetPageOfPublicRoomsResponse, error: ${error.message}")
          return
        }

        if (response.status != Status.Ok) {
          showErrorAlert("UserHasJoinedResponsePayload with non ok status ${response.status}")
          return
        }

        runLater {
          store.setPublicChatRoomList(response.publicChatRoomList)
          find<ConnectionToServerWindow>().replaceWith<ChatMainWindow>()
        }
      }
      else -> {
        //Do nothing
      }
    }
  }

  private fun increaseDelayAndGet(): Int {
    if (delayTime.get() < maxDelayTime) {
      return delayTime.addAndGet(2000)
    }

    return delayTime.get()
  }

  private fun changeConnectionStatus(content: String?) {
    runLater {
      connectionStatus.set(content ?: "Unknown error")
    }
  }
}