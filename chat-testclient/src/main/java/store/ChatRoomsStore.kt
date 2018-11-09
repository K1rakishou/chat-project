package store

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import manager.IdGeneratorManager
import model.chat_message.BaseChatMessageItem
import model.chat_room_list.BaseChatRoomListItem
import model.chat_room_list.NoRoomsNotificationItem
import model.chat_room_list.PublicChatRoomItem
import utils.ThreadChecker

class ChatRoomsStore {
  val selectedRoomStore = SelectedRoomStore()
  val publicChatRoomList: ObservableList<BaseChatRoomListItem> = FXCollections.observableArrayList()

  init {
    publicChatRoomList.add(NoRoomsNotificationItem.haveNotJoinedAnyRoomsYet())
  }

  private fun getChatRoomList(): List<PublicChatRoomItem> {
    ThreadChecker.throwIfNotOnMainThread()

    return publicChatRoomList
      .filterIsInstance(PublicChatRoomItem::class.java)
  }

  fun addManyChatRoomListItem(chatRoomItemList: List<BaseChatRoomListItem>) {
    ThreadChecker.throwIfNotOnMainThread()

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
    ThreadChecker.throwIfNotOnMainThread()

    when (chatRoomItem) {
      is NoRoomsNotificationItem -> {
        if (publicChatRoomList.isNotEmpty()) {
          throw RuntimeException("Must be empty!")
        }

        publicChatRoomList.add(0, chatRoomItem)
      }
      is PublicChatRoomItem -> {
        if (getChatRoomByName(chatRoomItem.roomName) != null) {
          throw IllegalStateException("Room with name ${chatRoomItem.roomName} already exists!")
        }

        publicChatRoomList.add(index, chatRoomItem)
      }
      else -> throw RuntimeException("Not implemented for ${chatRoomItem::class}")
    }
  }

  fun removeNoRoomsNotification() {
    ThreadChecker.throwIfNotOnMainThread()

    if (publicChatRoomList.isEmpty()) {
      return
    }

    if (publicChatRoomList[0].type == BaseChatRoomListItem.ChatRoomListItemType.NoRoomsNotificationType) {
      removeChatRoomListItem(0)
    }
  }

  fun removeChatRoomListItem(index: Int) {
    ThreadChecker.throwIfNotOnMainThread()

    publicChatRoomList.removeAt(index)
  }

  fun getChatRoomByName(roomName: String?): PublicChatRoomItem? {
    ThreadChecker.throwIfNotOnMainThread()

    if (roomName == null) {
      return null
    }

    return getChatRoomList().firstOrNull { it.roomName == roomName }
  }

  fun addChatRoomMessage(roomName: String, message: BaseChatMessageItem): Int {
    ThreadChecker.throwIfNotOnMainThread()

    val chatRoom = getChatRoomList().firstOrNull { it.roomName == roomName }
    if (chatRoom == null) {
      return -1
    }

    if (message.shouldUpdateIds()) {
      val messageId = IdGeneratorManager.getNextClientMessageId()
      val newChatMessage = BaseChatMessageItem.copyWithNewClientMessageId(message, messageId)
      chatRoom.addChatMessage(newChatMessage)
      return messageId
    }

    chatRoom.addChatMessage(message)

    //always return 0 for messages (like SystemMessage, or TextMessage when it was received from the server)
    //that won't be send to the server
    return 0
  }

  fun updateChatRoomMessageServerId(roomName: String, serverMessageId: Int, clientMessageId: Int) {
    ThreadChecker.throwIfNotOnMainThread()

    val chatRoom = getChatRoomList().firstOrNull { it.roomName == roomName }
    if (chatRoom == null) {
      return
    }

    val messageItemIndex = chatRoom.findChatMessageIndexByClientMessageId(clientMessageId)
    if (messageItemIndex == -1) {
      return
    }

    val oldMessageItem = chatRoom.getChatMessageByIndex(messageItemIndex)
    if (oldMessageItem == null) {
      return
    }

    val newChatMessage = BaseChatMessageItem.copyWithNewServerMessageId(oldMessageItem, serverMessageId)
    chatRoom.updateChatMessage(messageItemIndex, newChatMessage)
  }

  fun getChatRoomMessageHistory(roomName: String): ObservableList<BaseChatMessageItem> {
    ThreadChecker.throwIfNotOnMainThread()

    val chatRoom = requireNotNull(getChatRoomList().firstOrNull { it.roomName == roomName })
    return chatRoom.roomMessagesProperty
  }
}