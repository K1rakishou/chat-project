package controller

import ChatApp
import core.ResponseInfo
import core.ResponseType
import core.Status
import core.exception.ResponseDeserializationException
import core.packet.JoinChatRoomPacket
import core.response.JoinChatRoomResponsePayload
import core.security.SecurityUtils
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import manager.NetworkManager
import store.Store
import ui.chat_main_window.join_chat_room_dialog.JoinChatRoomDialogFragment

class JoinChatRoomDialogController : BaseController<JoinChatRoomDialogFragment>() {
  private val networkManager = ChatApp.networkManager
  private val store: Store by inject()

  override fun createController(viewParam: JoinChatRoomDialogFragment) {
    super.createController(viewParam)

    startListeningToPackets()
    networkManager.shouldReconnectOnDisconnect(true)
  }

  override fun destroyController() {
    super.destroyController()
  }

  fun joinChatRoom(chatRoomName: String, userName: String? = null, roomPassword: String? = null) {
    view.lockControls()

    if (store.isUserInRoom(chatRoomName)) {
      view.onError("Already in the room. Should not happen")
      return
    }

    if (!networkManager.isConnected) {
      view.onError("Not connected to the server")
      return
    }

    val userNameToSend = when {
      userName != null -> userName
      store.hasUserNameByRoomName(chatRoomName) -> store.getUserName(chatRoomName)
      else -> null
    }

    userNameToSend?.let { name ->
      val hashedPassword = if (roomPassword != null) {
        SecurityUtils.Hashing.sha3(roomPassword)
      } else {
        null
      }

      doOnBg {
        val packet = JoinChatRoomPacket(
          name,
          chatRoomName,
          hashedPassword)

        networkManager.sendPacket(packet)
      }
    }
  }

  private fun startListeningToPackets() {
    compositeDisposable += networkManager.responsesFlowable
      .filter { networkManager.isConnected }
      .subscribe(this::handleIncomingResponses, { it.printStackTrace() })

    compositeDisposable += networkManager.connectionStateObservable
      .filter { connectionState -> connectionState != NetworkManager.ConnectionState.Connected }
      .subscribeBy(onNext = { connectionState ->
        when (connectionState) {
          is NetworkManager.ConnectionState.Uninitialized -> {
            //Default state
          }
          is NetworkManager.ConnectionState.Connecting -> {
          }
          is NetworkManager.ConnectionState.Disconnected -> {
            view.lockControls()
          }
          is NetworkManager.ConnectionState.ErrorWhileTryingToConnect -> {
          }
          is NetworkManager.ConnectionState.Connected -> {
            view.unlockControls()
          }
          is NetworkManager.ConnectionState.Reconnected -> {
            view.unlockControls()
          }
        }
      })
  }

  private fun handleIncomingResponses(responseInfo: ResponseInfo) {
    when (responseInfo.responseType) {
      ResponseType.JoinChatRoomResponseType -> handleJoinChatRoomResponse(responseInfo)
      else -> {
        //Do nothing
      }
    }
  }

  private fun handleJoinChatRoomResponse(responseInfo: ResponseInfo) {
    println("JoinChatRoomResponseType response received")

    val response = try {
      JoinChatRoomResponsePayload.fromByteSink(responseInfo.byteSink)
    } catch (error: ResponseDeserializationException) {
      view.onError("Could not deserialize packet JoinChatRoomResponse, error: ${error.message}")
      return
    }

    if (response.status != Status.Ok) {
      if (response.status.isFatalError()) {
        throw RuntimeException("Fatal Error: ${response.status.toErrorMessage()}")
      }

      if (!response.status.belongsToJoinChatRoomResponse()) {
        throw IllegalStateException("The status code (${response.status}) does not belong to this response (JoinChatRoomResponsePayload)")
      }

      view.onFailedToJoinChatRoom(response.status)
      return
    }

    val roomName = response.roomName!!
    val userName = response.userName!!
    val users = response.users
    val messageHistory = response.messageHistory

    view.onJoinedToChatRoom(roomName, userName, users, messageHistory)
  }

}