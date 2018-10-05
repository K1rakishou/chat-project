package core.model.drainable.chat_message

import core.byte_sink.ByteSink
import core.sizeof

class TextChatMessage(
  id: Long,
  val senderName: String,
  val message: String
) : BaseChatMessage(id, ChatMessageType.Text) {

  override fun getSize(): Int {
    return super.getSize() + sizeof(senderName) + sizeof(message)
  }

  override fun serialize(sink: ByteSink) {
    super.serialize(sink)

    sink.writeString(senderName)
    sink.writeString(message)
  }
}