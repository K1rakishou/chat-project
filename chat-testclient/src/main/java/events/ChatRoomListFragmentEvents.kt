package events

import tornadofx.EventBus
import tornadofx.FXEvent


class ChatRoomListFragmentEvents {
  class SelectItem(val itemIndex: Int) : FXEvent(EventBus.RunOn.ApplicationThread)
  object ClearSelection : FXEvent(EventBus.RunOn.ApplicationThread)
}