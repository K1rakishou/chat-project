package model.chat_room_list

import core.exception.UnknownChatMessageTypeException
import core.model.drainable.PublicUserInChat
import core.model.drainable.chat_message.BaseChatMessage
import core.model.drainable.chat_message.ChatMessageType
import core.model.drainable.chat_message.TextChatMessage
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import model.chat_message.BaseChatMessageItem
import model.chat_message.ForeignTextChatMessageItem
import model.user.BaseUserItem
import model.user.ForeignUserItem
import model.user.MyUserItem
import java.lang.RuntimeException

class PublicChatRoomItem(
  roomName: String,
  imageUrl: String
) : BaseChatRoomListItem(ChatRoomListItemType.ChatRoomItemType, roomName) {

  private val imageUrlProperty by lazy { SimpleStringProperty(imageUrl) }
  fun imageUrlProperty() = imageUrlProperty
  var imageUrl: String
    get() = imageUrlProperty.get()
    set(value) = imageUrlProperty.set(value)

  val userListProperty = SimpleListProperty<BaseUserItem>(FXCollections.observableArrayList())
  val roomMessagesProperty = SimpleListProperty<BaseChatMessageItem>(FXCollections.observableArrayList())

  fun addMyUser(userName: String) {
    val user = MyUserItem(userName)

    if (userListProperty.contains(user)) {
      throw RuntimeException("Already contains user $userName")
    }

    userListProperty.add(user)
  }

  fun addForeignUser(userName: String) {
    val user = ForeignUserItem(userName)

    if (userListProperty.contains(user)) {
      throw RuntimeException("Already contains user $userName")
    }

    userListProperty.add(user)
  }

  fun isMyUserAdded(): Boolean {
    return userListProperty
      .any { it is MyUserItem }
  }

  fun getMyUser(): MyUserItem? {
    return userListProperty
      .asSequence()
      .filterIsInstance(MyUserItem::class.java)
      .firstOrNull()
  }

  fun getForeignUserList(): List<ForeignUserItem> {
    return userListProperty
      .filterIsInstance(ForeignUserItem::class.java)
  }

  fun replaceUserList(foreignUserList: List<PublicUserInChat>) {
    val mappedUsers = foreignUserList.map { user -> ForeignUserItem(user.userName) }

    userListProperty.clear()
    userListProperty.addAll(mappedUsers)
  }

  fun replaceChatRoomHistory(messageHistory: List<BaseChatMessage>) {
    val newChatRoomHistory = messageHistory.map { message ->
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

    roomMessagesProperty.clear()
    roomMessagesProperty.addAll(newChatRoomHistory)
  }

  //TODO: remove old messages when their count > MAX_CHAT_MESSAGES_COUNT
  fun addChatMessage(chatMessage: BaseChatMessageItem) {
    roomMessagesProperty.add(chatMessage)
  }

  fun findChatMessageByClientMessageId(clientMessageId: Int): BaseChatMessageItem? {
    return roomMessagesProperty.firstOrNull { it.clientMessageId == clientMessageId }
  }

  fun findChatMessageIndexByClientMessageId(clientMessageId: Int): Int {
    return roomMessagesProperty.indexOfFirst { it.clientMessageId == clientMessageId }
  }

  fun getChatMessageByIndex(index: Int): BaseChatMessageItem? {
    return roomMessagesProperty.getOrNull(index)
  }

  fun updateChatMessage(index: Int, newChatMessage: BaseChatMessageItem) {
    roomMessagesProperty[index] = newChatMessage
  }

  companion object {
    fun create(roomName: String, roomImageUrl: String): PublicChatRoomItem {
      return PublicChatRoomItem(roomName, roomImageUrl)
    }
  }
}