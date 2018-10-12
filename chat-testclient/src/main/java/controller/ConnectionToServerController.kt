package controller

import ChatApp
import core.ResponseInfo
import core.ResponseType
import core.Status
import core.exception.ResponseDeserializationException
import core.packet.GetPageOfPublicRoomsPacket
import core.response.GetPageOfPublicRoomsResponsePayload
import javafx.beans.property.SimpleStringProperty
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import manager.NetworkManager
import store.Store
import tornadofx.runLater
import ui.chat_main_window.ChatMainWindow
import ui.loading_window.ConnectionToServerWindow
import kotlin.coroutines.experimental.CoroutineContext

class ConnectionToServerController : BaseController(), CoroutineScope {
  private val networkManager = ChatApp.networkManager
  val store: Store by inject()

  lateinit var job: Job
  val connectionStatus = SimpleStringProperty("")
  val connectionError = SimpleStringProperty(null)
  var socketEventsChannel: ReceiveChannel<NetworkManager.SocketEvent>? = null

  override val coroutineContext: CoroutineContext
    get() = job
  override val isActive: Boolean
    get() = job.isActive

  fun createController() {
    job = Job()

    store.clearPublicChatRoomList()
    connectionError.set(null)
    connectionStatus.set("")
  }

  fun destroyController() {
    job.cancel()
    socketEventsChannel?.cancel()

    store.clearPublicChatRoomList()
  }

  fun startConnectionToServer(host: String, port: String) {
    changeConnectionStatus("Connecting...")
    networkManager.connect(host, port.toInt(), false)

    launch { startListeningToPackets() }
  }

  fun stopConnectionToServer() {
    networkManager.disconnect(false)

    goBackToConnectionWindow()
  }

  private suspend fun startListeningToPackets() {
    socketEventsChannel = networkManager.socketEventsQueue.openSubscription().apply {
      consumeEach { socketEvent ->
        when (socketEvent) {
          is NetworkManager.SocketEvent.ConnectedToServer -> {
            changeConnectionStatus("Connected")
            networkManager.sendPacket(GetPageOfPublicRoomsPacket(0, 20))
          }
          is NetworkManager.SocketEvent.ErrorWhileConnecting,
          is NetworkManager.SocketEvent.DisconnectedFromServer -> {
            if (socketEvent is NetworkManager.SocketEvent.ErrorWhileConnecting) {
              connectionError("Error while trying to connect: ${socketEvent.throwable.message}")
              println("ErrorWhileConnecting")
            } else {
              println("DisconnectedFromServer")
            }

            goBackToConnectionWindow()
          }
          is NetworkManager.SocketEvent.ResponseReceived -> {
            handleIncomingResponses(socketEvent.responseInfo)
          }
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

  private fun changeConnectionStatus(content: String?) {
    runLater {
      connectionStatus.set(content ?: "Unknown error")
    }
  }

  private fun connectionError(message: String) {
    runLater {
      connectionError.set(message)
    }
  }

  fun goBackToConnectionWindow() {
    runLater {
      find<ConnectionToServerWindow>().close()
    }
  }
}