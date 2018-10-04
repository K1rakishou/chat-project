package controller

import core.model.drainable.PublicChatRoom
import tornadofx.Controller

class Store : Controller() {
  private val publicChatRoomList = mutableListOf<PublicChatRoom>()
  private val joinedRooms = mutableSetOf<String>()

  @Synchronized
  fun setChatRoomList(chatRoomList: List<PublicChatRoom>) {
    publicChatRoomList.clear()
    publicChatRoomList.addAll(chatRoomList)
  }

  @Synchronized
  fun getChatRoomList(): List<PublicChatRoom> = publicChatRoomList

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
}