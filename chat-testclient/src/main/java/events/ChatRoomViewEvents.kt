package events

import tornadofx.EventBus
import tornadofx.FXEvent

class ChatRoomViewEvents {
  class ChangeSelectedRoom(val selectedRoomName: String) : FXEvent(EventBus.RunOn.ApplicationThread)
}