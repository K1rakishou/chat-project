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
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import kotlinx.coroutines.delay
import manager.NetworkManager
import model.chat_message.BaseChatMessageItem
import model.chat_message.ForeignTextChatMessageItem
import model.chat_message.MyTextChatMessageItem
import model.chat_message.SystemChatMessageItemMy
import model.chat_room_list.NoRoomsNotificationItem
import model.chat_room_list.PublicChatRoomItem
import store.ChatRoomsStore
import ui.chat_main_window.ChatMainWindow
import ui.chat_main_window.ChatRoomView
import ui.chat_main_window.ChatRoomViewEmpty
import utils.ThreadChecker
import java.util.concurrent.TimeUnit

class ChatMainWindowController : BaseController<ChatMainWindow>() {
  private val networkManager: NetworkManager by lazy { ChatApp.networkManager }
  private val store: ChatRoomsStore by lazy { ChatApp.chatRoomsStore }
  private val delayBeforeAddFirstChatRoomMessage = 250L
  private var selectedRoomName: String? = null

  lateinit var scrollToBottomFlag: SimpleIntegerProperty

  val lastChatMessageMap = FXCollections.observableHashMap<String, SimpleStringProperty>()
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
    ThreadChecker.throwIfNotOnMainThread()

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

    val chatRoom = store.getChatRoomByName(selectedRoomName)
    if (chatRoom == null) {
      println("Cannot send a message because chat room (${selectedRoomName}) does not exist")
      return
    }

    val myUser = chatRoom.getMyUser()
    if (myUser == null) {
      println("Cannot send a message because user does not exist in the room")
      return
    }

    val messageId = addChatMessage(selectedRoomName!!, MyTextChatMessageItem(myUser.userName, messageText))
    if (messageId == -1) {
      throw IllegalStateException("Could not add chat message (Probably selectedRoomName (${selectedRoomName}) refers to an unknown room)")
    }

    networkManager.sendPacket(SendChatMessagePacket(messageId, selectedRoomName!!, myUser.userName, messageText))
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
    doOnBg {
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
  }

  private fun handleNewChatMessageResponse(responseInfo: ResponseInfo) {
    println("NewChatMessageResponseType response received")
    ThreadChecker.throwIfOnMainThread()

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

    val userName = requireNotNull(response.userName)
    val message = requireNotNull(response.message)
    val roomName = requireNotNull(response.roomName)

    val messageItem = ForeignTextChatMessageItem(
      userName,
      message
    )

    doOnUI {
      addChatMessage(roomName, messageItem)
    }
  }

  private fun handleSendChatMessageResponse(responseInfo: ResponseInfo) {
    println("SendChatMessageResponseType response received")
    ThreadChecker.throwIfOnMainThread()

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

    val roomName = requireNotNull(response.roomName)
    val serverMessageId = response.serverMessageId
    val clientMessageId = response.clientMessageId

    doOnUI {
      store.updateChatRoomMessageServerId(roomName, serverMessageId, clientMessageId)
      //TODO: update sent message UI state here
    }
  }

  private fun handleUserHasJoinedResponse(responseInfo: ResponseInfo) {
    println("UserHasJoinedResponseType response received")
    ThreadChecker.throwIfOnMainThread()

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

    val roomName = requireNotNull(response.roomName)
    val userName = requireNotNull(response.user).userName

    doOnUI {
      addForeignUserToChatRoom(roomName, userName)
    }
  }

  private fun handleUserHasLeftResponse(responseInfo: ResponseInfo) {
    println("UserHasLeftResponseType response received")
    ThreadChecker.throwIfOnMainThread()

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

    val userName = requireNotNull(response.userName)
    val roomName = requireNotNull(response.roomName)

    doOnUI {
      removeForeignUserFromChatRoom(roomName, userName)
    }
  }

  private fun handleGetPageOfPublicRoomsResponse(responseInfo: ResponseInfo) {
    println("GetPageOfPublicRoomsResponseType response received")
    ThreadChecker.throwIfOnMainThread()

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
      updatePublicChatRoomList(response)
    }
  }

  private fun addForeignUserToChatRoom(roomName: String, userName: String) {
    val chatRoom = store.getChatRoomByName(roomName)
    if (chatRoom == null) {
      showErrorAlert("Room ${roomName} does not exist!")
      return
    }

    chatRoom.addForeignUser(userName)
    addChatMessage(roomName, SystemChatMessageItemMy("User \"$userName\" has joined to chat room"))
  }

  private fun removeForeignUserFromChatRoom(roomName: String, userName: String) {
    val chatRoom = store.getChatRoomByName(roomName)
    if (chatRoom == null) {
      showErrorAlert("Room ${roomName} does not exist!")
      return
    }

    chatRoom.removeUser(userName)
    addChatMessage(roomName, SystemChatMessageItemMy("User \"$userName\" has left the room"))
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

    updateLastChatRoomMessage(roomName, chatMessage)
    return messageId
  }

  private fun updateLastChatRoomMessage(roomName: String, chatMessage: BaseChatMessageItem) {
    when (chatMessage.getMessageType()) {
      BaseChatMessageItem.MessageType.MyTextMessage,
      BaseChatMessageItem.MessageType.ForeignTextMessage -> {
        doOnUI {
          val lastMessageText = when (chatMessage.getMessageType()) {
            BaseChatMessageItem.MessageType.MyTextMessage -> {
              chatMessage as MyTextChatMessageItem
              "${chatMessage.senderName}: ${chatMessage.messageText}"
            }
            BaseChatMessageItem.MessageType.ForeignTextMessage -> {
              chatMessage as ForeignTextChatMessageItem
              "${chatMessage.senderName}: ${chatMessage.messageText}"
            }
            else -> null
          }

          if (lastMessageText != null) {
            lastChatMessageMap[roomName]!!.set(lastMessageText)
          }
        }
      }
      else -> {
        //do nothing
      }
    }
  }

  private fun updatePublicChatRoomList(response: GetPageOfPublicRoomsResponsePayload) {
    if (response.publicChatRoomList.isEmpty()) {
      store.addChatRoomListItem(NoRoomsNotificationItem())
    } else {
      val mappedRooms = response.publicChatRoomList
        .map { PublicChatRoomItem.create(it.chatRoomName, it.chatRoomImageUrl) }

      store.removeNoRoomsNotification()
      store.addManyChatRoomListItem(mappedRooms)

      response.publicChatRoomList.forEach { chatRoom ->
        lastChatMessageMap[chatRoom.chatRoomName] = SimpleStringProperty("")
      }
    }
  }

  fun reloadRoomMessageHistory(roomName: String) {
    doOnUI {
      currentChatRoomMessageList.clear()

      val chatMessageHistory = store.getChatRoomMessageHistory(roomName)
      val lastMessage = chatMessageHistory.lastOrNull {
        it.getMessageType() == BaseChatMessageItem.MessageType.ForeignTextMessage ||
        it.getMessageType() == BaseChatMessageItem.MessageType.MyTextMessage
      }

      if (lastMessage != null) {
        updateLastChatRoomMessage(roomName, lastMessage)
      }

      currentChatRoomMessageList.addAll(chatMessageHistory)
    }
  }

  fun onChatRoomCreated(roomName: String, userName: String?, roomImageUrl: String) {
    doOnUI {
      lastChatMessageMap[roomName] = SimpleStringProperty()

      store.removeNoRoomsNotification()
      store.addChatRoomListItem(PublicChatRoomItem.create(roomName, roomImageUrl))

      //if userName is not null that means that we need to auto join this user into the created room
      if (userName != null) {
        onJoinedToChatRoom(roomName, userName, emptyList(), emptyList())

        view.selectRoomWithIndex(0)
        scrollChatToBottom()
      }
    }
  }

  fun onJoinedToChatRoom(roomName: String, userName: String, users: List<PublicUserInChat>, messageHistory: List<BaseChatMessage>) {
    doOnUI {
      val chatRoomViewEmpty = find<ChatRoomViewEmpty>()
      if (chatRoomViewEmpty.isDocked) {
        chatRoomViewEmpty.replaceWith<ChatRoomView>()
      }

      selectedRoomName = roomName

      //Wait some time before ChatRoomView shows up
      delay(TimeUnit.MILLISECONDS.toMillis(delayBeforeAddFirstChatRoomMessage))

      val chatRoom = requireNotNull(store.getChatRoomByName(roomName))
      chatRoom.replaceUserList(users)
      chatRoom.addMyUser(userName)
      chatRoom.replaceChatRoomHistory(messageHistory)

      reloadRoomMessageHistory(roomName)
      addChatMessage(roomName, SystemChatMessageItemMy("You've joined the chat room"))
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