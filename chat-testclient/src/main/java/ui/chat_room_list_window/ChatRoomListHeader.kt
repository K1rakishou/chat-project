package ui.chat_room_list_window

import tornadofx.*


class ChatRoomListHeader : View() {
  override val root = vbox {
    addClass(Styles.header)
    label("Public Rooms").setId(Styles.title)
  }
}