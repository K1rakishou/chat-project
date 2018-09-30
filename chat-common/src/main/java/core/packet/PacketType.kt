package core.packet

import java.lang.IllegalArgumentException

enum class PacketType(val value: Short) {
  SendECPublicKeyPacketType(0),
  CreateRoomPacketType(1),
  GetPageOfPublicRoomsPacketType(2);

  companion object {
    fun fromShort(type: Short): PacketType {
      return PacketType.values().firstOrNull { it.value == type }
        ?: throw IllegalArgumentException("Unknown packetType: $type")
    }
  }
}