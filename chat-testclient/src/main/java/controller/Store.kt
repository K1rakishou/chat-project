package controller

import core.model.drainable.PublicChatRoom
import tornadofx.Controller

class Store : Controller() {
  private val publicChatRoomList = mutableListOf<PublicChatRoom>()

  @Synchronized
  fun setChatRoomList(chatRoomList: List<PublicChatRoom>) {
    publicChatRoomList.clear()
    publicChatRoomList.addAll(chatRoomList)
  }

  @Synchronized
  fun getChatRoomList(): List<PublicChatRoom> = publicChatRoomList
}