package core

import java.lang.IllegalArgumentException

enum class ResponseType(val value: Short) {
  CreateRoomResponseType(0),
  GetPageOfPublicRoomsResponseType(1),
  JoinChatRoomResponseType(2),
  UserHasJoinedResponseType(3),
  SendChatMessageResponseType(4),
  NewChatMessageResponseType(5),
  UserHasLeftResponseType(6),
  SearchChatRoomResponseType(7);

  companion object {
    fun fromShort(type: Short): ResponseType {
      return ResponseType.values().firstOrNull { it.value == type }
        ?: throw IllegalArgumentException("Unknown packetType: $type")
    }
  }
}