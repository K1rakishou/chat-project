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
  val messageId: Int,
  val messageType: ChatMessageType
) : CanBeDrainedToSink, CanMeasureSizeOfFields {

  override fun getSize(): Int {
    return sizeof(messageId) + sizeof(messageType.value)
  }

  override fun serialize(sink: ByteSink) {
    sink.writeInt(messageId)
    sink.writeByte(messageType.value)
  }

  companion object : CanBeRestoredFromSink {
    override fun <T> createFromByteSink(byteSink: ByteSink): T? {
      val messageId = byteSink.readInt()
      val messageType = ChatMessageType.fromByte(byteSink.readByte())

      when (messageType) {
        ChatMessageType.Text -> {
          val senderName = byteSink.readString(Constants.maxUserNameLen)
            ?: throw ResponseDeserializationException("Could not read BaseChatMessage.senderName")
          val message = byteSink.readString(Constants.maxTextMessageLen)
            ?: throw ResponseDeserializationException("Could not read BaseChatMessage.message")

          return TextChatMessage(messageId, senderName, message) as T
        }
        ChatMessageType.Unknown -> throw UnknownChatMessageTypeException()
      }
    }
  }
}