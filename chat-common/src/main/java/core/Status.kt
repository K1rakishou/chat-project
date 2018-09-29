package core

enum class Status(val value: Short) {
  ChatRoomWithThisNameAlreadyExists(-2),
  UnknownError(-1),
  Ok(0);

  companion object {
    fun fromShort(value: Short): Status {
      return Status.values().first { it.value == value }
    }
  }
}