package controller

import core.model.drainable.PublicChatRoom
import core.model.drainable.PublicUserInChat
import tornadofx.Controller

class Store : Controller() {
  private val publicChatRoomList = mutableListOf<ChatRoom>()
  private val usersInChatRoom = mutableListOf<UserInChat>()
  private val joinedRooms = mutableSetOf<String>()

  @Synchronized
  fun setChatRoomList(chatRoomList: List<PublicChatRoom>) {
    publicChatRoomList.clear()
    publicChatRoomList.addAll(chatRoomList.map { chatRoom -> ChatRoom(chatRoom.roomName, chatRoom.usersCount) })
  }

  @Synchronized
  fun getChatRoomList(): List<ChatRoom> = publicChatRoomList

  @Synchronized
  fun addJoinedRoom(roomName: String) {
    joinedRooms += roomName
  }

  @Synchronized
  fun removeJoindRoom(roomName: String) {
    joinedRooms -= roomName
  }

  @Synchronized
  fun isAlreadyJoined(roomName: String): Boolean {
    return joinedRooms.contains(roomName)
  }

  @Synchronized
  fun addUserInChatRoom(user: PublicUserInChat) {
    usersInChatRoom.add(UserInChat(user.userName, user.ecPublicKey))
  }

  @Synchronized
  fun addUserInChatRoomList(userList: List<PublicUserInChat>) {
    usersInChatRoom.addAll(userList.map { user -> UserInChat(user.userName, user.ecPublicKey) })
  }

  data class ChatRoom(
    val roomName: String,
    val usersCount: Short,
    val messageHistory: List<String> = mutableListOf()
  ) {
    fun getRoomMessagesAsString(): String {
      return buildString {
        for (message in messageHistory) {
          append(message)
          append('\n')
        }
      }
    }
  }

  data class UserInChat(
    val userName: String,
    val ecPublicKey: ByteArray
  )
}