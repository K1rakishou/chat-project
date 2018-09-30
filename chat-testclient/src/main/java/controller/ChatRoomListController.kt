package controller

import core.Status
import core.packet.GetPageOfPublicRoomsPacketPayload
import core.response.GetPageOfPublicRoomsResponsePayload
import kotlinx.coroutines.experimental.launch
import model.PublicChatRoomItem
import tornadofx.Controller
import tornadofx.SortedFilteredList
import ui.ChatApp

class ChatRoomListController : Controller() {
  val networkManager = (app as ChatApp).networkManager
  val chatRooms = SortedFilteredList<PublicChatRoomItem>()

  init {
    launch { startListeningToPackets() }
    launch { networkManager.sendPacket(GetPageOfPublicRoomsPacketPayload(0, 20)) }
  }

  private suspend fun startListeningToPackets() {
    for (response in networkManager.responseQueue.openSubscription()) {
      when (response) {
        is GetPageOfPublicRoomsResponsePayload -> {
          if (response.status == Status.Ok) {
            chatRooms.clear()
            chatRooms.addAll(response.publicChatRoomList.map { PublicChatRoomItem(it.roomName, it.usersCount) })
          }
        }
      }
    }
  }
}