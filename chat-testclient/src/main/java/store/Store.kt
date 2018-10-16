package store

import core.exception.UnknownChatMessageTypeException
import core.model.drainable.PublicChatRoom
import core.model.drainable.PublicUserInChat
import core.model.drainable.chat_message.BaseChatMessage
import core.model.drainable.chat_message.ChatMessageType
import core.model.drainable.chat_message.TextChatMessage
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import model.PublicChatRoomItem
import model.PublicUserInChatItem
import model.chat_message.BaseChatMessageItem
import model.chat_message.TextChatMessageItem
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

  fun getJoinedRoomsList() = ArrayList(joinedRooms)

  fun isUserInRoom(roomName: String): Boolean {
    return joinedRooms.any { it == roomName }
  }

  fun setChatRoomUserList(roomName: String, userList: List<PublicUserInChat>) {
    val chatRoom = requireNotNull(publicChatRoomList.firstOrNull { it.roomName == roomName })
    val convertedUserList = userList.map { user -> PublicUserInChatItem(user.userName) }

    chatRoom.userListProperty().clear()
    chatRoom.userListProperty().addAll(convertedUserList)
  }

  fun setChatRoomMessageList(roomName: String, messageHistory: List<BaseChatMessage>) {
    val chatRoom = requireNotNull(publicChatRoomList.firstOrNull { it.roomName == roomName })
    val convertedMessageList = messageHistory.map { message ->
      when (message.messageType) {
        ChatMessageType.Unknown -> throw UnknownChatMessageTypeException(message.messageType)
        ChatMessageType.Text -> {
          message as TextChatMessage
          TextChatMessageItem(message.senderName, message.message)
        }
      }
    }

    chatRoom.roomMessagesProperty().clear()
    chatRoom.roomMessagesProperty().addAll(convertedMessageList)
  }

  fun addChatRoomMessage(roomName: String, message: BaseChatMessageItem): Boolean {
    val chatRoom = publicChatRoomList.firstOrNull { it.roomName == roomName }
    if (chatRoom == null) {
      return false
    }

    chatRoom.roomMessagesProperty().add(message)
    return true
  }

  fun getChatRoomMessageHistory(roomName: String): ObservableList<BaseChatMessageItem> {
    val chatRoom = requireNotNull(publicChatRoomList.firstOrNull { it.roomName == roomName })
    return chatRoom.roomMessagesProperty()
  }
}