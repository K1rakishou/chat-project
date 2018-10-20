package controller

import ChatApp
import core.ResponseInfo
import core.ResponseType
import core.Status
import core.exception.ResponseDeserializationException
import core.model.drainable.PublicUserInChat
import core.model.drainable.chat_message.BaseChatMessage
import core.packet.GetPageOfPublicRoomsPacket
import core.packet.SendChatMessagePacket
import core.response.*
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.FXCollections
import kotlinx.coroutines.delay
import manager.NetworkManager
import model.PublicChatRoomItem
import model.chat_message.BaseChatMessageItem
import model.chat_message.ForeignTextChatMessageItem
import model.chat_message.MyTextChatMessageItem
import model.chat_message.SystemChatMessageItemMy
import store.Store
import ui.chat_main_window.ChatMainWindow
import ui.chat_main_window.ChatRoomView
import ui.chat_main_window.ChatRoomViewEmpty
import java.util.concurrent.TimeUnit

class ChatMainWindowController : BaseController<ChatMainWindow>() {
  private val networkManager = ChatApp.networkManager
  private val store: Store by inject()
  private val delayBeforeAddFirstChatRoomMessage = 250L
  private var selectedRoomName: String? = null

  lateinit var scrollToBottomFlag: SimpleIntegerProperty

  val publicChatRoomList = FXCollections.observableArrayList<PublicChatRoomItem>()
  val currentChatRoomMessageList = FXCollections.observableArrayList<BaseChatMessageItem>()

  override fun createController(viewParam: ChatMainWindow) {
    super.createController(viewParam)

    scrollToBottomFlag = SimpleIntegerProperty(0)

    startListeningToPackets()
    networkManager.shouldReconnectOnDisconnect(true)
    networkManager.sendPacket(GetPageOfPublicRoomsPacket(0, 20))
  }

  override fun destroyController() {
    selectedRoomName = null

    super.destroyController()
  }

  fun updateSelectedRoom(roomName: String) {
    selectedRoomName = roomName
  }

  fun sendMessage(messageText: String) {
    if (!networkManager.isConnected) {
      println("Not connected")

      selectedRoomName?.let { roomName ->
        addChatMessage(roomName, SystemChatMessageItemMy("Not connected to the server"))
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

    val messageId = addChatMessage(selectedRoomName!!, MyTextChatMessageItem(store.getUserName(selectedRoomName), messageText))
    if (messageId == -1) {
      throw IllegalStateException("Could not add chat message (Probably selectedRoomName (${selectedRoomName}) refers to an unknown room)")
    }

    doOnBg {
      networkManager.sendPacket(SendChatMessagePacket(messageId, selectedRoomName!!, store.getUserName(selectedRoomName), messageText))
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
      ResponseType.GetPageOfPublicRoomsResponseType -> handleGetPageOfPublicRoomsResponse(responseInfo)
      ResponseType.UserHasJoinedResponseType -> handleUserHasJoinedResponse(responseInfo)
      ResponseType.SendChatMessageResponseType -> handleSendChatMessageResponse(responseInfo)
      ResponseType.NewChatMessageResponseType -> handleNewChatMessageResponse(responseInfo)
      ResponseType.UserHasLeftResponseType -> handleUserHasLeftResponse(responseInfo)
      else -> {
        //Do nothing
      }
    }
  }

  private fun handleUserHasLeftResponse(responseInfo: ResponseInfo) {
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

    addChatMessage(response.roomName!!, SystemChatMessageItemMy("User \"${response.userName!!}\" has left the room"))
  }

  private fun handleNewChatMessageResponse(responseInfo: ResponseInfo) {
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

    val message = ForeignTextChatMessageItem(
      response.userName!!,
      response.message!!
    )

    addChatMessage(response.roomName!!, message)
  }

  private fun handleSendChatMessageResponse(responseInfo: ResponseInfo) {
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

    val roomName = response.roomName!!
    val serverMessageId = response.serverMessageId
    val clientMessageId = response.clientMessageId

    store.updateChatRoomMessageServerId(roomName, serverMessageId, clientMessageId)

    //TODO: update sent message UI state here
  }

  private fun handleUserHasJoinedResponse(responseInfo: ResponseInfo) {
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

    addChatMessage(response.roomName!!, SystemChatMessageItemMy("User \"${response.user!!.userName}\" has joined to chat room"))
  }

  private fun handleGetPageOfPublicRoomsResponse(responseInfo: ResponseInfo) {
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

    doOnUI {
      store.setPublicChatRoomList(response.publicChatRoomList)

      publicChatRoomList.addAll(store.getPublicChatRoomList())
    }
  }

  private fun addChatMessage(roomName: String, chatMessage: BaseChatMessageItem): Int {
    val messageId = store.addChatRoomMessage(roomName, chatMessage)
    if (messageId == -1) {
      println("Could not add chat room message to room with name $roomName")
      return -1
    }

    selectedRoomName?.let { name ->
      if (name == roomName) {
        doOnUI {
          currentChatRoomMessageList.add(chatMessage)
          scrollChatToBottom()
        }
      }
    }

    return messageId
  }

  fun reloadRoomMessageHistory(roomName: String) {
    doOnUI {
      currentChatRoomMessageList.clear()
      currentChatRoomMessageList.addAll(store.getChatRoomMessageHistory(roomName))
    }
  }

  fun loadRoomInfo(roomName: String, userName: String, users: List<PublicUserInChat>, messageHistory: List<BaseChatMessage>) {
    doOnUI {
      val chatRoomViewEmpty = find<ChatRoomViewEmpty>()
      if (chatRoomViewEmpty.isDocked) {
        chatRoomViewEmpty.replaceWith<ChatRoomView>()
      }

      //Wait some time before ChatRoomView shows up
      delay(TimeUnit.MILLISECONDS.toMillis(delayBeforeAddFirstChatRoomMessage))

      store.addJoinedRoom(roomName)
      store.setChatRoomUserList(roomName, users)
      store.loadChatRoomMessageHistory(roomName, messageHistory)
      store.addUserToRoom(roomName, userName)

      reloadRoomMessageHistory(roomName)
      addChatMessage(roomName, SystemChatMessageItemMy("You've joined the chat room"))

      scrollChatToBottom()
    }
  }

  fun onChatRoomCreated(roomName: String, userName: String?, roomImageUrl: String) {
    doOnUI {
      store.addPublicChatRoom(roomName, roomImageUrl)

      //if userName is not null that means that we need to auto join this user into the created room
      if (userName != null) {
        val chatRoomViewEmpty = find<ChatRoomViewEmpty>()
        if (chatRoomViewEmpty.isDocked) {
          chatRoomViewEmpty.replaceWith<ChatRoomView>()
        }

        //Wait some time before ChatRoomView shows up
        delay(TimeUnit.MILLISECONDS.toMillis(delayBeforeAddFirstChatRoomMessage))

        selectedRoomName = roomName

        store.addJoinedRoom(roomName)
        store.setChatRoomUserList(roomName, FXCollections.observableArrayList())
        store.addUserToRoom(roomName, userName)

        view.selectRoomWithIndex(0)
      }

      publicChatRoomList.add(0, PublicChatRoomItem(
        roomName,
        roomImageUrl,
        FXCollections.observableArrayList(),
        FXCollections.observableArrayList())
      )

      if (userName != null) {
        reloadRoomMessageHistory(roomName)
        addChatMessage(roomName, SystemChatMessageItemMy("You've joined the chat room"))

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
    doOnUI {
      selectedRoomName?.let { roomName ->
        addChatMessage(roomName, SystemChatMessageItemMy("Disconnected from the server"))
      }
    }
  }

  private fun onReconnected() {
    doOnUI {
      selectedRoomName?.let { roomName ->
        addChatMessage(roomName, SystemChatMessageItemMy("Reconnected"))
      }
    }
  }

  private fun onErrorWhileTryingToConnect(error: Throwable?) {
    doOnUI {
      selectedRoomName?.let { roomName ->
        addChatMessage(roomName, SystemChatMessageItemMy("Error while trying to reconnect: ${error?.message
          ?: "No error message"}"))
      }
    }
  }

}