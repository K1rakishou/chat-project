package events

import tornadofx.EventBus
import tornadofx.FXEvent

object ChatRoomListClearRoomSelectionEvent : FXEvent(EventBus.RunOn.BackgroundThread)