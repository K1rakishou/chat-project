package core.packet

import java.lang.IllegalArgumentException

enum class PacketType(val value: Short) {
  SendECPublicKeyPacketPayload(0),
  CreateRoomPacketPayload(1);

  companion object {
    fun fromShort(type: Short): PacketType {
      return PacketType.values().firstOrNull { it.value == type }
        ?: throw IllegalArgumentException("Unknown packetType: $type")
    }
  }
}