package model.chat_room_list

import java.util.*

abstract class BaseChatRoomListItem(
  val type: ChatRoomListItemType
) {
  val id = UUID.randomUUID()

  enum class ChatRoomListItemType {
    ChatRoomItemType,
    NoRoomsNotificationType
  }
}