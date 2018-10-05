package core.model.drainable.chat_message

enum class ChatMessageType(val value: Byte) {
  Unknown(-1),
  Text(0);

  companion object {
    fun fromByte(value: Byte): ChatMessageType {
      return ChatMessageType.values().firstOrNull { it.value == value } ?: Unknown
    }
  }
}