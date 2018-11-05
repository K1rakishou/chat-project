package core

import java.lang.IllegalArgumentException

enum class PacketType(val value: Short) {
  CreateRoomPacketType(0),
  GetPageOfPublicRoomsPacketType(1),
  JoinRoomPacketType(2),
  SendChatMessagePacketType(3),
  SearchChatRoomPacketType(4);

  companion object {
    fun fromShort(type: Short): PacketType {
      return PacketType.values().firstOrNull { it.value == type }
        ?: throw IllegalArgumentException("Unknown packetType: $type")
    }
  }
}