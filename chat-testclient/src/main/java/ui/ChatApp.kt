package ui

import manager.NetworkManager
import tornadofx.App
import ui.chat_room_list_window.ChatRoomListWindow

class ChatApp : App(ChatRoomListWindow::class) {
  val networkManager = NetworkManager().also { it.run() }
}