package model.chat_room_list

class NoRoomsNotificationItem
  : BaseChatRoomListItem(ChatRoomListItemType.NoRoomsNotificationType, noRoomsNotificationChatRoomName) {

  companion object {
    const val noRoomsNotificationChatRoomName = "no_rooms_notification"
  }
}