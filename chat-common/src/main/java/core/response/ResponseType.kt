package core.response

import java.lang.IllegalArgumentException

enum class ResponseType(val value: Short) {
  SendECPublicKeyResponseType(0),
  CreateRoomResponseType(1),
  GetPageOfPublicRoomsResponseType(2);

  companion object {
    fun fromShort(type: Short): ResponseType {
      return ResponseType.values().firstOrNull { it.value == type }
        ?: throw IllegalArgumentException("Unknown packetType: $type")
    }
  }
}