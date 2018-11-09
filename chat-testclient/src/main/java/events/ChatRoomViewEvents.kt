package events

import tornadofx.EventBus
import tornadofx.FXEvent

class ChatRoomViewEvents {
  object ScrollToBottom : FXEvent(EventBus.RunOn.ApplicationThread)
}