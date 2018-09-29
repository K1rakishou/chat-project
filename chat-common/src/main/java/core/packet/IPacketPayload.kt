package core.packet

import java.nio.ByteBuffer

interface IPacketPayload {
  fun getPacketType(): PacketType
  fun getPacketVersion(): Int
  fun getPayloadSize(): Int
  fun toByteBuffer(): ByteBuffer
}