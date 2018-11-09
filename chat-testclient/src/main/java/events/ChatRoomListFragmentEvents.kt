package events

import tornadofx.EventBus
import tornadofx.FXEvent


class ChatRoomListFragmentEvents {
  object ClearSelection : FXEvent(EventBus.RunOn.ApplicationThread)
  object ClearSearchInput : FXEvent(EventBus.RunOn.ApplicationThread)
}