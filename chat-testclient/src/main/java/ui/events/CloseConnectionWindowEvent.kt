package ui.events

import tornadofx.EventBus
import tornadofx.FXEvent

object CloseConnectionWindowEvent : FXEvent(EventBus.RunOn.BackgroundThread)