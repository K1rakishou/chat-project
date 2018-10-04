package core

enum class Status(val value: Short) {
  WrongRoomPassword(-6),
  BadParam(-5),
  CouldNotJoinChatRoom(-4),
  ChatRoomDoesNotExist(-3),
  ChatRoomAlreadyExists(-2),
  UnknownError(-1),
  Ok(0);

  companion object {
    fun fromShort(value: Short): Status {
      return Status.values().first { it.value == value }
    }
  }
}