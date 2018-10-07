package controller

import core.exception.UnknownChatMessageTypeException
import core.model.drainable.PublicChatRoom
import core.model.drainable.PublicUserInChat
import core.model.drainable.chat_message.BaseChatMessage
import core.model.drainable.chat_message.ChatMessageType
import core.model.drainable.chat_message.TextChatMessage
import core.security.SecurityUtils
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import model.CurrentUser
import model.PublicChatRoomItem
import model.PublicUserInChatItem
import model.chat_message.BaseChatMessageItem
import model.chat_message.TextChatMessageItem
import tornadofx.Controller

class Store : Controller() {
  private val currentUser: SimpleObjectProperty<CurrentUser> = SimpleObjectProperty(CurrentUser("test user ${SecurityUtils.Generator.generateRandomString(6)}"))
  private val publicChatRoomList: ObservableList<PublicChatRoomItem> = FXCollections.observableArrayList()
  private val joinedRooms: ObservableList<String> = FXCollections.observableArrayList()

  fun getCurrentUserName(): String {
    return currentUser.get().userName
  }

  fun getPublicChatRoomList(): ObservableList<PublicChatRoomItem> {
    return publicChatRoomList
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
    val convertedUserList = userList.map { user -> PublicUserInChatItem(user.userName, user.ecPublicKey) }

    chatRoom.userListProperty().get().clear()
    chatRoom.userListProperty().get().addAll(convertedUserList)
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

    chatRoom.roomMessagesProperty().get().clear()
    chatRoom.roomMessagesProperty().get().addAll(convertedMessageList)
  }

  fun addChatRoomMessage(roomName: String, message: BaseChatMessageItem) {
    val chatRoom = requireNotNull(publicChatRoomList.firstOrNull { it.roomName == roomName })
    chatRoom.roomMessagesProperty().add(message)
  }

  fun getChatRoomMessageHistory(roomName: String): ObservableList<BaseChatMessageItem> {
    val chatRoom = requireNotNull(publicChatRoomList.firstOrNull { it.roomName == roomName })
    return chatRoom.roomMessagesProperty()
  }
}