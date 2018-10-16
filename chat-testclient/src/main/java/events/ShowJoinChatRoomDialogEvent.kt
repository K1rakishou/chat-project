package events

import tornadofx.EventBus
import tornadofx.FXEvent

class ShowJoinChatRoomDialogEvent(
  val roomName: String
) : FXEvent(EventBus.RunOn.BackgroundThread)