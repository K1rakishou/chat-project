package core.packet

import core.PositionAwareByteArray

abstract class AbstractPacketPayload {
  abstract val packetVersion: Short
  abstract fun getPacketType(): PacketType
  abstract fun getPayloadSize(): Int
  abstract fun toByteArray(): PositionAwareByteArray
}