package model.chat_room_list

import javafx.beans.property.SimpleStringProperty

class NoRoomsNotificationItem private constructor(
  message: String,
  val notificationType: NotificationType
) : BaseChatRoomListItem(ChatRoomListItemType.NoRoomsNotificationType, noRoomsNotificationChatRoomName) {

  private val messageProperty by lazy { SimpleStringProperty(message) }
  fun messageProperty() = messageProperty
  var message: String
    get() = messageProperty.get()
    set(value) = messageProperty.set(value)

  enum class NotificationType {
    JoinedRoomsNotificationType,
    SearchRoomsNotificationType
  }

  companion object {
    const val noRoomsNotificationChatRoomName = "no_rooms_notification"

    fun haveNotJoinedAnyRoomsYet(): NoRoomsNotificationItem {
      return NoRoomsNotificationItem("You have not joined any rooms yet", NotificationType.JoinedRoomsNotificationType)
    }

    fun noRoomsWereFound(): NoRoomsNotificationItem {
      return NoRoomsNotificationItem("No rooms were found", NotificationType.SearchRoomsNotificationType)
    }
  }
}