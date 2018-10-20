package events

import tornadofx.EventBus
import tornadofx.FXEvent


class ChatRoomListFragmentEvents {
  class SelectListViewItem(val itemIndex: Int) : FXEvent(EventBus.RunOn.ApplicationThread)
}