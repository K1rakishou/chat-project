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
import store.ChatRoomsStore
import ui.chat_main_window.join_chat_room_dialog.JoinChatRoomDialogFragment
import utils.ThreadChecker

class JoinChatRoomDialogController : BaseController<JoinChatRoomDialogFragment>() {
  private val networkManager: NetworkManager by lazy { ChatApp.networkManager }
  private val store: ChatRoomsStore by lazy { ChatApp.chatRoomsStore }

  override fun createController(viewParam: JoinChatRoomDialogFragment) {
    super.createController(viewParam)

    startListeningToPackets()
  }

  override fun destroyController() {
    super.destroyController()
  }

  fun joinChatRoom(chatRoomName: String, userName: String, roomPassword: String? = null) {
    view.lockControls()

    val chatRoom = store.getChatRoomByName(chatRoomName)
    if (chatRoom != null) {
      if (chatRoom.isMyUserAdded()) {
        view.onError("Already in the room. Should not happen")
        return
      }
    }

    if (!networkManager.isConnected) {
      view.onError("Not connected to the server")
      return
    }

    val hashedPassword = if (roomPassword != null && roomPassword.isNotEmpty()) {
      SecurityUtils.Hashing.sha3(roomPassword)
    } else {
      null
    }

    val packet = JoinChatRoomPacket(
      userName,
      chatRoomName,
      hashedPassword)

    networkManager.sendPacket(packet)
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
    doOnBg {
      when (responseInfo.responseType) {
        ResponseType.JoinChatRoomResponseType -> handleJoinChatRoomResponse(responseInfo)
        else -> {
          //Do nothing
        }
      }
    }
  }

  private fun handleJoinChatRoomResponse(responseInfo: ResponseInfo) {
    println("JoinChatRoomResponseType response received")
    ThreadChecker.throwIfOnMainThread()

    val response = try {
      JoinChatRoomResponsePayload.fromByteSink(responseInfo.byteSink)
    } catch (error: ResponseDeserializationException) {
      view.onError("Could not deserialize packet JoinChatRoomResponse, error: ${error.message}")
      return
    }

    if (response.status != Status.Ok) {
      if (response.status.isFatalError()) {
        view.onError("Fatal Error: ${response.status.toErrorMessage()}")
        return
      }

      if (!response.status.belongsToJoinChatRoomResponse()) {
        view.onError("The status code (${response.status}) does not belong to this response (JoinChatRoomResponsePayload)")
        return
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