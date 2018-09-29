package core

enum class Status(val value: Short) {
  ChatRoomWithThisNameAlreadyExists(-2),
  UnknownError(-1),
  Ok(0);
}