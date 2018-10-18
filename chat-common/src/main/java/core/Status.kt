package core

import java.lang.IllegalArgumentException

enum class Status(val value: Short) {
  UserNameAlreadyTaken(-9),
  BadPacket(-8),
  UserDoesNotExistInTheRoom(-7),
  WrongRoomPassword(-6),
  BadParam(-5),
  CouldNotJoinChatRoom(-4),
  ChatRoomDoesNotExist(-3),
  ChatRoomAlreadyExists(-2),
  UnknownError(-1),
  Ok(0);

  fun toErrorMessage(): String {
    return when (this) {
      UserNameAlreadyTaken -> "User name is already taken"
      BadPacket -> "Bad packet received from the server"
      UserDoesNotExistInTheRoom -> "User does not exist in the room"
      WrongRoomPassword -> "Wrong room password"
      BadParam -> "Bad parameter"
      CouldNotJoinChatRoom -> "Could not join chat room for some unknown reason (try again later)"
      ChatRoomDoesNotExist -> "Chat room does not exist"
      ChatRoomAlreadyExists -> "Chat room already exists"
      UnknownError -> "Unknown error"
      Ok -> throw IllegalArgumentException("Not an error!")
    }
  }

  fun isFatalError(): Boolean {
    return when (this) {
      BadPacket,
      UnknownError -> true

      UserNameAlreadyTaken,
      UserDoesNotExistInTheRoom,
      WrongRoomPassword,
      BadParam,
      CouldNotJoinChatRoom,
      ChatRoomDoesNotExist,
      ChatRoomAlreadyExists,
      Ok -> false
    }
  }

  fun belongsToJoinChatRoomResponse(): Boolean {
    return when (this) {
      UnknownError,
      Ok,
      UserNameAlreadyTaken,
      BadPacket,
      WrongRoomPassword,
      BadParam,
      CouldNotJoinChatRoom,
      ChatRoomDoesNotExist -> true
      ChatRoomAlreadyExists,
      UserDoesNotExistInTheRoom -> false
    }
  }

  companion object {
    fun fromShort(value: Short): Status {
      return Status.values().first { it.value == value }
    }
  }
}