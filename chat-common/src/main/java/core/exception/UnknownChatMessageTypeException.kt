package core.exception

import core.model.drainable.chat_message.ChatMessageType
import java.lang.Exception

class UnknownChatMessageTypeException(
  messageType: ChatMessageType
) : Exception("Unknown chat message type (${messageType.value})")