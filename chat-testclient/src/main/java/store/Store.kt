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
import model.chat_room_list.PublicChatRoomItem
import model.PublicUserInChatItem
import model.chat_message.BaseChatMessageItem
import model.chat_message.ForeignTextChatMessageItem
import model.chat_room_list.BaseChatRoomListItem
import model.chat_room_list.NoRoomsNotificationItem
import tornadofx.Controller
import java.lang.RuntimeException

class Store : Controller() {
  private val userNameByRoomName = hashMapOf<String, String>()
  private val publicChatRoomList: ObservableList<BaseChatRoomListItem> = FXCollections.observableArrayList()
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

  fun addJoinedRoom(roomName: String) {
    joinedRooms.add(roomName)
  }

  fun isUserInRoom(roomName: String): Boolean {
    return joinedRooms.any { it == roomName }
  }

  fun addManyChatRoomListItem(chatRoomItemList: List<BaseChatRoomListItem>) {
    for (item in chatRoomItemList) {
      val index = if (publicChatRoomList.isEmpty()) {
        0
      } else {
        publicChatRoomList.lastIndex
      }

      addChatRoomListItem(item, index)
    }
  }

  fun addChatRoomListItem(chatRoomItem: BaseChatRoomListItem, index: Int = 0) {
    when (chatRoomItem) {
      is NoRoomsNotificationItem -> {
        if (publicChatRoomList.isNotEmpty()) {
          throw RuntimeException("Must be empty!")
        }

        publicChatRoomList.add(0, chatRoomItem)
      }
      is PublicChatRoomItem -> {
        if (getPublicChatRoom(chatRoomItem.roomName) != null) {
          throw IllegalStateException("Room with name ${chatRoomItem.roomName} already exists!")
        }

        publicChatRoomList.add(index, chatRoomItem)
      }
      else -> throw RuntimeException("Not implemented for ${chatRoomItem::class}")
    }
  }

  fun removeNoRoomsNotification() {
    if (publicChatRoomList.isEmpty()) {
      return
    }

    if (publicChatRoomList[0].type == BaseChatRoomListItem.ChatRoomListItemType.NoRoomsNotificationType) {
      removeChatRoomListItem(0)
    }
  }

  fun removeChatRoomListItem(index: Int) {
    publicChatRoomList.removeAt(index)
  }

  fun getPublicChatRoomList(): ObservableList<BaseChatRoomListItem> {
    return publicChatRoomList
  }

  fun getPublicChatRoom(roomName: String): PublicChatRoomItem? {
    val filtered = publicChatRoomList
      .filterIsInstance(PublicChatRoomItem::class.java)

    return filtered.firstOrNull { it.roomName == roomName }
  }

  fun setChatRoomUserList(roomName: String, userList: List<PublicUserInChat>) {
    val filtered = publicChatRoomList
      .filterIsInstance(PublicChatRoomItem::class.java)

    val chatRoom = requireNotNull(filtered.firstOrNull { it.roomName == roomName })
    val convertedUserList = userList.map { user -> PublicUserInChatItem(user.userName) }

    chatRoom.userListProperty().clear()
    chatRoom.userListProperty().addAll(convertedUserList)
  }

  fun loadChatRoomMessageHistory(roomName: String, messageHistory: List<BaseChatMessage>) {
    val filtered = publicChatRoomList
      .filterIsInstance(PublicChatRoomItem::class.java)

    val chatRoom = requireNotNull(filtered.firstOrNull { it.roomName == roomName })
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
    val filtered = publicChatRoomList
      .filterIsInstance(PublicChatRoomItem::class.java)

    val chatRoom = filtered.firstOrNull { it.roomName == roomName }
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

    //always return 0 for messages (like SystemMessage, or TextMessage when it was received from the server)
    //that won't be send to the server
    return 0
  }

  fun updateChatRoomMessageServerId(roomName: String, serverMessageId: Int, clientMessageId: Int) {
    val filtered = publicChatRoomList
      .filterIsInstance(PublicChatRoomItem::class.java)

    val chatRoom = filtered.firstOrNull { it.roomName == roomName }
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
    val filtered = publicChatRoomList
      .filterIsInstance(PublicChatRoomItem::class.java)

    val chatRoom = requireNotNull(filtered.firstOrNull { it.roomName == roomName })
    return chatRoom.roomMessagesProperty()
  }
}