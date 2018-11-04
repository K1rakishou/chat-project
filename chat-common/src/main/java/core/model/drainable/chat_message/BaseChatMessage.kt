package core.model.drainable.chat_message

import core.Constants
import core.byte_sink.ByteSink
import core.exception.ResponseDeserializationException
import core.exception.UnknownChatMessageTypeException
import core.interfaces.CanBeDrainedToSink
import core.interfaces.CanBeRestoredFromSink
import core.interfaces.CanMeasureSizeOfFields
import core.sizeof

abstract class BaseChatMessage(
  val serverMessageId: Int,
  val clientMessageId: Int,
  val messageType: ChatMessageType,
  val isMyMessage: Boolean
) : CanBeDrainedToSink, CanMeasureSizeOfFields {

  override fun getSize(): Int {
    return sizeof(serverMessageId) + sizeof(clientMessageId) + sizeof(messageType.value) + sizeof(isMyMessage)
  }

  override fun serialize(sink: ByteSink) {
    sink.writeInt(serverMessageId)
    sink.writeInt(clientMessageId)
    sink.writeByte(messageType.value)
    sink.writeBoolean(isMyMessage)
  }

  companion object : CanBeRestoredFromSink {
    override fun <T> createFromByteSink(byteSink: ByteSink): T? {
      val serverMessageId = byteSink.readInt()
      val clientMessageId = byteSink.readInt()
      val messageType = ChatMessageType.fromByte(byteSink.readByte())
      val isMyMessage = byteSink.readBoolean()

      when (messageType) {
        ChatMessageType.Text -> {
          val senderName = byteSink.readString(Constants.maxUserNameLen)
            ?: throw ResponseDeserializationException("Could not read BaseChatMessage.senderName")
          val message = byteSink.readString(Constants.maxTextMessageLen)
            ?: throw ResponseDeserializationException("Could not read BaseChatMessage.message")

          return TextChatMessage(isMyMessage, serverMessageId, clientMessageId, senderName, message) as T
        }
        ChatMessageType.Unknown -> throw UnknownChatMessageTypeException(messageType)
      }
    }
  }
}