package events

import tornadofx.EventBus
import tornadofx.FXEvent

object ShowJoinChatRoomDialog : FXEvent(EventBus.RunOn.BackgroundThread)