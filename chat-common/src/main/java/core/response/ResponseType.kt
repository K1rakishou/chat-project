package core.response

import java.lang.IllegalArgumentException

enum class ResponseType(val value: Short) {
  SendECPublicKeyResponse(0),
  CreateRoomResponse(1);

  companion object {
    fun fromShort(type: Short): ResponseType {
      return ResponseType.values().firstOrNull { it.value == type }
        ?: throw IllegalArgumentException("Unknown packetType: $type")
    }
  }
}