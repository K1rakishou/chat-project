package core.model.drainable.chat_message

import core.byte_sink.ByteSink
import core.exception.UnknownChatMessageType
import core.interfaces.CanBeDrainedToSink
import core.interfaces.CanBeRestoredFromSink
import core.interfaces.CanMeasureSizeOfFields
import core.sizeof

abstract class BaseChatMessage(
  val id: Long,
  val messageType: ChatMessageType
) : CanBeDrainedToSink, CanMeasureSizeOfFields {

  override fun getSize(): Int {
    return sizeof(id) + sizeof(messageType.value)
  }

  override fun serialize(sink: ByteSink) {
    sink.writeLong(id)
    sink.writeByte(messageType.value)
  }

  companion object : CanBeRestoredFromSink {
    override fun <T> createFromByteSink(byteSink: ByteSink): T? {
      val id = byteSink.readLong()
      val messageType = ChatMessageType.fromByte(byteSink.readByte())

      when (messageType) {
        ChatMessageType.Text -> {
          val senderName = byteSink.readString()
          val message = byteSink.readString()

          return TextChatMessage(id, senderName!!, message!!) as T
        }
        ChatMessageType.Unknown -> throw UnknownChatMessageType()
      }
    }
  }
}