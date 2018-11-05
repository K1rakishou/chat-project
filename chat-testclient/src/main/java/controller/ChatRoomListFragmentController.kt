package controller

import core.Constants
import core.packet.SearchChatRoomPacket
import manager.NetworkManager
import ui.chat_main_window.ChatRoomListFragment

class ChatRoomListFragmentController : BaseController<ChatRoomListFragment>() {
  private val networkManager: NetworkManager by lazy { ChatApp.networkManager }

  override fun createController(viewParam: ChatRoomListFragment) {
    super.createController(viewParam)
  }

  override fun destroyController() {
    super.destroyController()
  }

  fun sendSearchRequest(chatRoomNameToSearch: String) {
    if (chatRoomNameToSearch.isBlank()) {
      println("chatRoomNameToSearch is blank")
      return
    }

    if (chatRoomNameToSearch.length > Constants.maxChatRoomNameLength) {
      println("chatRoomNameToSearch length (${chatRoomNameToSearch.length}) exceeds Constants.maxChatRoomNameLength (${Constants.maxChatRoomNameLength})")
      return
    }

    networkManager.sendPacket(SearchChatRoomPacket(chatRoomNameToSearch))
  }
}