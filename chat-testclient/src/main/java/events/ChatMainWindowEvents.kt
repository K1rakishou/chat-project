package events

import core.model.drainable.PublicUserInChat
import core.model.drainable.chat_message.BaseChatMessage
import tornadofx.EventBus
import tornadofx.FXEvent

class ChatMainWindowEvents {

  class ShowJoinChatRoomDialogEvent(
    val roomName: String
  ) : FXEvent(EventBus.RunOn.ApplicationThread)

  object ShowCreateChatRoomDialogEvent : FXEvent(EventBus.RunOn.ApplicationThread)

  class JoinedChatRoomEvent(
    val roomName: String,
    val userName: String,
    val users: List<PublicUserInChat>,
    val messageHistory: List<BaseChatMessage>
  ) : FXEvent(EventBus.RunOn.ApplicationThread)

  class ChatRoomCreatedEvent(
    val roomName: String,
    val userName: String?,
    val roomImageUrl: String
  ) : FXEvent(EventBus.RunOn.ApplicationThread)
}