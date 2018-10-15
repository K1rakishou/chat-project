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
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.FXCollections
import javafx.util.Duration
import kotlinx.coroutines.experimental.launch
import manager.NetworkManager
import model.PublicChatRoomItem
import model.chat_message.BaseChatMessageItem
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
  val currentChatRoomMessageHistory = FXCollections.observableArrayList<BaseChatMessageItem>()

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
        addChatMessage(roomName, TextChatMessageItem.systemMessage("Not connected to the server"))
      }

      return
    }

    if (selectedRoomName == null) {
      println("Cannot send a message since no room is selected")
      return
    }

    if (!store.isUserInRoom(selectedRoomName!!)) {
      println("Cannot send a message since user is not in the room")
      return
    }

    addChatMessage(selectedRoomName!!, TextChatMessageItem(store.getCurrentUserName(), messageText))

    launch {
      networkManager.sendPacket(SendChatMessagePacket(0, selectedRoomName!!, store.getCurrentUserName(), messageText))
    }
  }

  fun joinChatRoom(publicChatRoomItem: PublicChatRoomItem) {
    selectedRoomName = publicChatRoomItem.roomName

    if (store.isUserInRoom(publicChatRoomItem.roomName)) {
      setRoomMessageHistory(publicChatRoomItem.roomName)
      return
    }

    if (!networkManager.isConnected) {
      println("Not connected")

//      selectedRoomName?.let { roomName ->
//        val chatRoom = store.getPublicChatRoom(roomName)
//      }

      return
    }

    launch {
      val packet = JoinChatRoomPacket(
        store.getCurrentUserName(),
        publicChatRoomItem.roomName,
        null)

      networkManager.sendPacket(packet)
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
          is NetworkManager.ConnectionState.Uninitialized -> {
            //Default state
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
          addChatMessageToAllRooms(TextChatMessageItem.systemMessage("Could not deserialize packet JoinChatRoomResponse, error: ${error.message}"))
          return
        }

        if (response.status != Status.Ok) {
          showErrorAlert("Error while trying to join a chat room")
          return
        }

        val roomName = response.roomName!!
        val users = response.users
        val messageHistory = response.messageHistory

        loadRoomInfo(roomName, users, messageHistory)
      }
      ResponseType.UserHasJoinedResponseType -> {
        println("UserHasJoinedResponseType response received")

        val response = try {
          UserHasJoinedResponsePayload.fromByteSink(responseInfo.byteSink)
        } catch (error: ResponseDeserializationException) {
          addChatMessageToAllRooms(TextChatMessageItem.systemMessage("Could not deserialize packet UserHasJoinedResponse, error: ${error.message}"))
          return
        }
        if (response.status != Status.Ok) {
          showErrorAlert("UserHasJoinedResponsePayload with non ok status ${response.status}")
          return
        }

        addChatMessage(response.roomName!!, TextChatMessageItem.systemMessage("User \"${response.user!!.userName}\" has joined to chat room"))
      }
      ResponseType.SendChatMessageResponseType -> {
        println("SendChatMessageResponseType response received")

        val response = try {
          SendChatMessageResponsePayload.fromByteSink(responseInfo.byteSink)
        } catch (error: ResponseDeserializationException) {
          addChatMessageToAllRooms(TextChatMessageItem.systemMessage("Could not deserialize packet SendChatMessageResponse, error: ${error.message}"))
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
          addChatMessageToAllRooms(TextChatMessageItem.systemMessage("Could not deserialize packet NewChatMessageResponse, error: ${error.message}"))
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
          addChatMessageToAllRooms(TextChatMessageItem.systemMessage("Could not deserialize packet UserHasLeftResponsePayload, error: ${error.message}"))
          return
        }

        if (response.status != Status.Ok) {
          showErrorAlert("UserHasLeftResponsePayload with non ok status ${response.status}")
          return
        }

        addChatMessage(response.roomName!!, TextChatMessageItem.systemMessage("User \"${response.userName!!}\" has left the room"))
      }
      else -> {
        //Do nothing
      }
    }
  }

  private fun addChatMessage(roomName: String, chatMessage: BaseChatMessageItem) {
    if (!store.addChatRoomMessage(roomName, chatMessage)) {
      //TODO: add to an inner queue and re-send upon reconnection
      return
    }

    runLater {
      currentChatRoomMessageHistory.add(chatMessage)
      store.addChatRoomMessage(roomName, chatMessage)
      scrollChatToBottom()
    }
  }

  private fun addChatMessageToAllRooms(chatMessage: TextChatMessageItem) {
    store.getJoinedRoomsList().forEach { joinedRoomName ->
      addChatMessage(joinedRoomName, chatMessage)
    }
  }

  private fun setRoomMessageHistory(roomName: String) {
    runLater {
      currentChatRoomMessageHistory.clear()
      currentChatRoomMessageHistory.addAll(store.getChatRoomMessageHistory(roomName))
    }
  }

  private fun loadRoomInfo(roomName: String, users: List<PublicUserInChat>, messageHistory: List<BaseChatMessage>) {
    runLater {
      find<ChatRoomViewEmpty>().replaceWith<ChatRoomView>()

      //Wait some time before ChatRoomView shows up
      runLater(Duration.millis(delayBeforeAddFirstChatRoomMessage)) {
        store.addChatRoomMessage(roomName, TextChatMessageItem.systemMessage("You've joined the chat room"))

        store.addJoinedRoom(roomName)
        store.setChatRoomUserList(roomName, users)
        store.setChatRoomMessageList(roomName, messageHistory)

        setRoomMessageHistory(roomName)
        scrollChatToBottom()
      }
    }
  }

  private fun scrollChatToBottom() {
    //To make the listener receive the new value we need it to be different from the last value.
    //Otherwise it doesn't work
    scrollToBottomFlag.set(scrollToBottomFlag.value + 1)
  }

  private fun onDisconnected() {
    runLater {
      selectedRoomName?.let { roomName ->
        addChatMessage(roomName, TextChatMessageItem.systemMessage("Disconnected from the server"))
      }
    }
  }

  private fun onErrorWhileTryingToConnect(error: Throwable?) {
    runLater {
      selectedRoomName?.let { roomName ->
        addChatMessage(roomName, TextChatMessageItem.systemMessage("Error while trying to reconnect: ${error?.message ?: "No error message"}"))
      }
    }
  }
}