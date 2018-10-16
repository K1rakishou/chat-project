package events

import tornadofx.EventBus
import tornadofx.FXEvent

class JoinChatRoomInfoEvent(
  val canceled: Boolean = false,
  val roomName: String,
  val userName: String,
  val roomPassword: String? = null
) : FXEvent(EventBus.RunOn.BackgroundThread)