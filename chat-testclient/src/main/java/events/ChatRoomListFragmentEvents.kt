package events

import tornadofx.EventBus
import tornadofx.FXEvent


class ChatRoomListFragmentEvents {
  class SelectItem(val key: String) : FXEvent(EventBus.RunOn.ApplicationThread)
  object ClearSelection : FXEvent(EventBus.RunOn.ApplicationThread)
}