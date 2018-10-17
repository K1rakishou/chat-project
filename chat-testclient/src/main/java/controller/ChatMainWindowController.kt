package controller

import ChatApp
import core.ResponseInfo
import core.ResponseType
import core.Status
import core.exception.ResponseDeserializationException
import core.model.drainable.PublicUserInChat
import core.model.drainable.chat_message.BaseChatMessage
import core.packet.GetPageOfPublicRoomsPacket
import core.packet.JoinChatRoomPacket
import core.packet.SendChatMessagePacket
import core.response.*
import core.security.SecurityUtils
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.FXCollections
import javafx.util.Duration
import kotlinx.coroutines.experimental.launch
import manager.NetworkManager
import model.PublicChatRoomItem
import model.chat_message.BaseChatMessageItem
import model.chat_message.SystemChatMessageItem
import model.chat_message.TextChatMessageItem
import store.Store
import tornadofx.runLater
import ui.chat_main_window.ChatRoomView
import ui.chat_main_window.ChatRoomViewEmpty

class ChatMainWindowController : BaseController() {
  private val networkManager = ChatApp.networkManager
  private val store: Store by inject()
  private val delayBeforeAddFirstChatRoomMessage = 250.0
  private var selectedRoomName: String? = null

  lateinit var scrollToBottomFlag: SimpleIntegerProperty

  val publicChatRoomList = FXCollections.observableArrayList<PublicChatRoomItem>()
  val currentChatRoomMessageList = FXCollections.observableArrayList<BaseChatMessageItem>()

  override fun createController() {
    super.createController()

    scrollToBottomFlag = SimpleIntegerProperty(0)

    startListeningToPackets()
    networkManager.shouldReconnectOnDisconnect(true)
    networkManager.sendPacket(GetPageOfPublicRoomsPacket(0, 20))
  }

  override fun destroyController() {
    selectedRoomName = null

    super.destroyController()
  }

  fun sendMessage(messageText: String) {
    if (!networkManager.isConnected) {
      println("Not connected")

      selectedRoomName?.let { roomName ->
        addChatMessage(roomName, SystemChatMessageItem("Not connected to the server"))
      }

      return
    }

    if (selectedRoomName == null) {
      println("Cannot send a message because no room is selected")
      return
    }

    if (!store.isUserInRoom(selectedRoomName!!)) {
      println("Cannot send a message because user does not exist in the room")
      return
    }

    addChatMessage(selectedRoomName!!, TextChatMessageItem(store.getUserName(selectedRoomName), messageText))

    launch {
      networkManager.sendPacket(SendChatMessagePacket(0, selectedRoomName!!, store.getUserName(selectedRoomName), messageText))
    }
  }

  fun joinChatRoom(chatRoomName: String, userName: String? = null, roomPassword: String? = null) {
    selectedRoomName = chatRoomName

    selectedRoomName?.let { roomName ->
      if (store.isUserInRoom(roomName)) {
        replaceRoomMessageHistory(roomName)
        return
      }

      if (!networkManager.isConnected) {
        println("Not connected")
        return
      }

      val userNameToSend = when {
        userName != null -> userName
        store.hasUserNameByRoomName(chatRoomName) -> store.getUserName(chatRoomName)
        else -> null
      }

      userNameToSend?.let { name ->
        val hashedPassword = if (roomPassword != null) {
          SecurityUtils.Hashing.sha3(roomPassword.toByteArray())
        } else {
          null
        }

        launch {
          val packet = JoinChatRoomPacket(
            name,
            roomName,
            hashedPassword)

          networkManager.sendPacket(packet)
        }
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
            onDisconnected()
          }
          is NetworkManager.ConnectionState.ErrorWhileTryingToConnect -> {
            onErrorWhileTryingToConnect(connectionState.error)
          }
          is NetworkManager.ConnectionState.Connected -> {
          }
          is NetworkManager.ConnectionState.Reconnected -> {
            onReconnected()
          }
        }
      })
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

          publicChatRoomList.clear()
          publicChatRoomList.addAll(store.getPublicChatRoomList())
        }
      }
      ResponseType.JoinChatRoomResponseType -> {
        println("JoinChatRoomResponseType response received")

        val response = try {
          JoinChatRoomResponsePayload.fromByteSink(responseInfo.byteSink)
        } catch (error: ResponseDeserializationException) {
          showErrorAlert("Could not deserialize packet JoinChatRoomResponse, error: ${error.message}")
          return
        }

        if (response.status != Status.Ok) {
          showErrorAlert("Error while trying to join a chat room")
          return
        }

        val roomName = response.roomName!!
        val userName = response.userName!!
        val users = response.users
        val messageHistory = response.messageHistory

        loadRoomInfo(roomName, userName, users, messageHistory)
      }
      ResponseType.UserHasJoinedResponseType -> {
        println("UserHasJoinedResponseType response received")

        val response = try {
          UserHasJoinedResponsePayload.fromByteSink(responseInfo.byteSink)
        } catch (error: ResponseDeserializationException) {
          showErrorAlert("Could not deserialize packet UserHasJoinedResponse, error: ${error.message}")
          return
        }
        if (response.status != Status.Ok) {
          showErrorAlert("UserHasJoinedResponsePayload with non ok status ${response.status}")
          return
        }

        addChatMessage(response.roomName!!, SystemChatMessageItem("User \"${response.user!!.userName}\" has joined to chat room"))
      }
      ResponseType.SendChatMessageResponseType -> {
        println("SendChatMessageResponseType response received")

        val response = try {
          SendChatMessageResponsePayload.fromByteSink(responseInfo.byteSink)
        } catch (error: ResponseDeserializationException) {
          showErrorAlert("Could not deserialize packet SendChatMessageResponse, error: ${error.message}")
          return
        }

        if (response.status != Status.Ok) {
          showErrorAlert("SendChatMessageResponseType with non ok status ${response.status}")
          return
        }
      }
      ResponseType.NewChatMessageResponseType -> {
        println("NewChatMessageResponseType response received")

        val response = try {
          NewChatMessageResponsePayload.fromByteSink(responseInfo.byteSink)
        } catch (error: ResponseDeserializationException) {
          showErrorAlert("Could not deserialize packet NewChatMessageResponse, error: ${error.message}")
          return
        }

        if (response.status != Status.Ok) {
          showErrorAlert("NewChatMessageResponsePayload with non ok status ${response.status}")
          return
        }

        addChatMessage(response.roomName!!, TextChatMessageItem(response.userName!!, response.message!!))
      }
      ResponseType.UserHasLeftResponseType -> {
        println("UserHasLeftResponseType response received")

        val response = try {
          UserHasLeftResponsePayload.fromByteSink(responseInfo.byteSink)
        } catch (error: ResponseDeserializationException) {
          showErrorAlert("Could not deserialize packet UserHasLeftResponsePayload, error: ${error.message}")
          return
        }

        if (response.status != Status.Ok) {
          showErrorAlert("UserHasLeftResponsePayload with non ok status ${response.status}")
          return
        }

        addChatMessage(response.roomName!!, SystemChatMessageItem("User \"${response.userName!!}\" has left the room"))
      }
      else -> {
        //Do nothing
      }
    }
  }

  private fun addChatMessage(roomName: String, chatMessage: BaseChatMessageItem) {
    if (!store.addChatRoomMessage(roomName, chatMessage)) {
      println("Could not add chat room message to room with name $roomName")
      return
    }

    runLater {
      currentChatRoomMessageList.add(chatMessage)
      scrollChatToBottom()
    }
  }

  private fun replaceRoomMessageHistory(roomName: String) {
    runLater {
      currentChatRoomMessageList.clear()
      currentChatRoomMessageList.addAll(store.getChatRoomMessageHistory(roomName))
    }
  }

  private fun loadRoomInfo(roomName: String, userName: String, users: List<PublicUserInChat>, messageHistory: List<BaseChatMessage>) {
    runLater {
      find<ChatRoomViewEmpty>().replaceWith<ChatRoomView>()

      //Wait some time before ChatRoomView shows up
      runLater(Duration.millis(delayBeforeAddFirstChatRoomMessage)) {
        store.addJoinedRoom(roomName)
        store.setChatRoomUserList(roomName, users)
        store.setChatRoomMessageList(roomName, messageHistory)
        store.addUserToRoom(roomName, userName)

        replaceRoomMessageHistory(roomName)
        addChatMessage(roomName, SystemChatMessageItem("You've joined the chat room"))

        scrollChatToBottom()
      }
    }
  }

  //TODO: change to eventbus
  private fun scrollChatToBottom() {
    //To make the listener receive the new value we need it to be different from the last value.
    //Otherwise it doesn't work
    scrollToBottomFlag.set(scrollToBottomFlag.value + 1)
  }

  private fun onDisconnected() {
    runLater {
      selectedRoomName?.let { roomName ->
        addChatMessage(roomName, SystemChatMessageItem("Disconnected from the server"))
      }
    }
  }

  private fun onReconnected() {
    runLater {
      selectedRoomName?.let { roomName ->
        addChatMessage(roomName, SystemChatMessageItem("Reconnected"))
      }
    }
  }

  private fun onErrorWhileTryingToConnect(error: Throwable?) {
    runLater {
      selectedRoomName?.let { roomName ->
        addChatMessage(roomName, SystemChatMessageItem("Error while trying to reconnect: ${error?.message
          ?: "No error message"}"))
      }
    }
  }
}