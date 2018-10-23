package controller

import core.ResponseInfo
import core.ResponseType
import core.Status
import core.exception.ResponseDeserializationException
import core.packet.CreateRoomPacket
import core.response.CreateRoomResponsePayload
import core.security.SecurityUtils
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import manager.NetworkManager
import ui.chat_main_window.create_chat_room_dialog.CreateChatRoomDialogFragment

class CreateChatRoomDialogController : BaseController<CreateChatRoomDialogFragment>() {
  private val networkManager: NetworkManager by lazy { ChatApp.networkManager }
  private var roomToBeCreated: ChatRoomToBeCreatedTempInfo? = null

  override fun createController(viewParam: CreateChatRoomDialogFragment) {
    super.createController(viewParam)

    startListeningToPackets()
    networkManager.shouldReconnectOnDisconnect(true)
  }

  override fun destroyController() {
    super.destroyController()
  }

  fun createChatRoom(roomName: String, roomPassword: String?, roomImageUrl: String, userName: String?, isPublic: Boolean) {
    view.lockControls()

    if (!networkManager.isConnected) {
      view.onError("Not connected to the server")
      return
    }

    if (roomToBeCreated != null) {
      println("Request is already in progress")
      return
    }

    val passwordHash = if (roomPassword != null && roomPassword.isNotEmpty()) {
      SecurityUtils.Hashing.sha3(roomPassword)
    } else {
      null
    }

    val name = if (userName.isNullOrEmpty()) {
      null
    } else {
      userName
    }

    roomToBeCreated = ChatRoomToBeCreatedTempInfo(roomName, passwordHash, roomImageUrl, name, isPublic)
    networkManager.sendPacket(CreateRoomPacket(isPublic, roomName, passwordHash, roomImageUrl, name))
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
      ResponseType.CreateRoomResponseType -> handleCreateRoomResponse(responseInfo)
      else -> {
        //Do nothing
      }
    }
  }

  private fun handleCreateRoomResponse(responseInfo: ResponseInfo) {
    try {
      println("CreateRoomResponseType response received")

      val response = try {
        CreateRoomResponsePayload.fromByteSink(responseInfo.byteSink)
      } catch (error: ResponseDeserializationException) {
        view.onError("Could not deserialize packet CreateRoomResponse, error: ${error.message}")
        return
      }

      if (response.status != Status.Ok) {
        if (response.status.isFatalError()) {
          view.onError("Fatal Error: ${response.status.toErrorMessage()}")
          return
        }

        if (!response.status.belongsToCreateRoomResponse()) {
          view.onError("The status code (${response.status}) does not belong to this response (CreateRoomResponsePayload)")
          return
        }

        view.onFailedToCreateChatRoom(response.status)
        return
      }

      requireNotNull(roomToBeCreated)
      println("Chat room has been created")

      view.onChatRoomCreated(
        roomToBeCreated!!.roomName,
        roomToBeCreated!!.roomPassword,
        roomToBeCreated!!.roomImageUrl,
        roomToBeCreated!!.userName,
        roomToBeCreated!!.isPublic
      )
    } finally {
      roomToBeCreated = null
    }
  }

  data class ChatRoomToBeCreatedTempInfo(
    val roomName: String,
    val roomPassword: String?,
    val roomImageUrl: String,
    val userName: String?,
    val isPublic: Boolean
  )
}