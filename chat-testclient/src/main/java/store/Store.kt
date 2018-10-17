package store

import core.exception.UnknownChatMessageTypeException
import core.model.drainable.PublicChatRoom
import core.model.drainable.PublicUserInChat
import core.model.drainable.chat_message.BaseChatMessage
import core.model.drainable.chat_message.ChatMessageType
import core.model.drainable.chat_message.TextChatMessage
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import manager.IdGeneratorManager
import model.PublicChatRoomItem
import model.PublicUserInChatItem
import model.chat_message.BaseChatMessageItem
import model.chat_message.ForeignTextChatMessageItem
import tornadofx.Controller

class Store : Controller() {
  private val userNameByRoomName = hashMapOf<String, String>()
  private val publicChatRoomList: ObservableList<PublicChatRoomItem> = FXCollections.observableArrayList()
  private val joinedRooms: ObservableList<String> = FXCollections.observableArrayList()

  fun getUserName(roomName: String?): String {
    if (roomName == null) {
      throw IllegalStateException("User has not joined the room $roomName")
    }

    return userNameByRoomName[roomName]
      ?: throw IllegalStateException("User has not joined the room $roomName")
  }

  fun hasUserNameByRoomName(roomName: String): Boolean {
    return userNameByRoomName.containsKey(roomName)
  }

  fun addUserToRoom(roomName: String, userName: String) {
    userNameByRoomName[roomName] = userName
  }

  fun getPublicChatRoomList(): ObservableList<PublicChatRoomItem> {
    return publicChatRoomList
  }

  fun getPublicChatRoom(roomName: String): PublicChatRoomItem? {
    return publicChatRoomList.firstOrNull { it.roomName == roomName }
  }

  fun setPublicChatRoomList(list: List<PublicChatRoom>) {
    publicChatRoomList.clear()
    publicChatRoomList.addAll(list.map { chatRoom ->
      PublicChatRoomItem(chatRoom.chatRoomName, chatRoom.usersCount, FXCollections.observableArrayList(), FXCollections.observableArrayList())
    })
  }

  fun clearPublicChatRoomList() {
    publicChatRoomList.clear()
  }

  fun addJoinedRoom(roomName: String) {
    joinedRooms.add(roomName)
  }

  fun isUserInRoom(roomName: String): Boolean {
    return joinedRooms.any { it == roomName }
  }

  fun setChatRoomUserList(roomName: String, userList: List<PublicUserInChat>) {
    val chatRoom = requireNotNull(publicChatRoomList.firstOrNull { it.roomName == roomName })
    val convertedUserList = userList.map { user -> PublicUserInChatItem(user.userName) }

    chatRoom.userListProperty().clear()
    chatRoom.userListProperty().addAll(convertedUserList)
  }

  fun loadChatRoomMessageHistory(roomName: String, messageHistory: List<BaseChatMessage>) {
    val chatRoom = requireNotNull(publicChatRoomList.firstOrNull { it.roomName == roomName })
    val convertedMessageList = messageHistory.map { message ->
      when (message.messageType) {
        ChatMessageType.Unknown -> throw UnknownChatMessageTypeException(message.messageType)
        ChatMessageType.Text -> {
          message as TextChatMessage

          //TODO: check every message's clientId whether it exists in the roomMessages list.
          //Depending on it - create either ForeignTextChatMessageItem (if it does not exists there)
          //or MyTextChatMessageItem (if it does)
          ForeignTextChatMessageItem(message.senderName, message.message)
        }
      }
    }

    chatRoom.roomMessagesProperty().clear()
    chatRoom.roomMessagesProperty().addAll(convertedMessageList)
  }

  fun addChatRoomMessage(roomName: String, message: BaseChatMessageItem): Int {
    val chatRoom = publicChatRoomList.firstOrNull { it.roomName == roomName }
    if (chatRoom == null) {
      return -1
    }

    if (message.shouldUpdateIds()) {
      val messageId = IdGeneratorManager.getNextClientMessageId()
      val newChatMessage = BaseChatMessageItem.copyWithNewClientMessageId(message, messageId)
      chatRoom.roomMessagesProperty().add(newChatMessage)
      return messageId
    }

    chatRoom.roomMessagesProperty().add(message)

    //always return 0 for messages (like SystemMessage, or TextMessage but when it was received from the server)
    //that won't be send to the server
    return 0
  }

  fun updateChatRoomMessageServerId(roomName: String, serverMessageId: Int, clientMessageId: Int) {
    val chatRoom = publicChatRoomList.firstOrNull { it.roomName == roomName }
    if (chatRoom == null) {
      return
    }

    val messageItemIndex = chatRoom.roomMessagesProperty()
      .indexOfFirst { it.clientMessageId == clientMessageId }

    if (messageItemIndex == -1) {
      return
    }

    val oldMessageItem = chatRoom.roomMessagesProperty().get(messageItemIndex)
    val newChatMessage = BaseChatMessageItem.copyWithNewServerMessageId(oldMessageItem, serverMessageId)

    chatRoom.roomMessagesProperty().set(messageItemIndex, newChatMessage)
  }

  fun getChatRoomMessageHistory(roomName: String): ObservableList<BaseChatMessageItem> {
    val chatRoom = requireNotNull(publicChatRoomList.firstOrNull { it.roomName == roomName })
    return chatRoom.roomMessagesProperty()
  }
}