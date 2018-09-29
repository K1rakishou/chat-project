package core

enum class Status(val value: Short) {
  PacketQueueIsFull(-2),
  UnknownError(-1),
  Ok(0);
}