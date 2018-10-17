package events

import tornadofx.EventBus
import tornadofx.FXEvent

class JoinChatRoomInfoEvent private constructor(
  val canceled: Boolean = false,
  val roomName: String,
  val userName: String,
  val roomPassword: String? = null
) : FXEvent(EventBus.RunOn.BackgroundThread) {

  companion object {
    fun createOk(roomName: String, userName: String, roomPassword: String? = null): JoinChatRoomInfoEvent {
      return JoinChatRoomInfoEvent(false, roomName, userName, roomPassword)
    }

    fun createCanceled(): JoinChatRoomInfoEvent {
      return JoinChatRoomInfoEvent(true, "", "", null)
    }
  }
}