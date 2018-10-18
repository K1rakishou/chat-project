package events

import tornadofx.EventBus
import tornadofx.FXEvent

class ConnectionWindowEvents {
  object CloseConnectionWindowEvent : FXEvent(EventBus.RunOn.ApplicationThread)
}