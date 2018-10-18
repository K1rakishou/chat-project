package events

import tornadofx.EventBus
import tornadofx.FXEvent

class ChatRoomListFragmentEvents {
  object ClearRoomSelectionEvent : FXEvent(EventBus.RunOn.ApplicationThread)
}