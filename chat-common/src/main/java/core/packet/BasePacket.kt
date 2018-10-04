package core.packet

import core.Packet
import core.PacketType
import core.byte_sink.ByteSink
import core.sizeof

abstract class BasePacket {
  abstract val packetVersion: Short
  abstract fun getPacketType(): PacketType
  abstract fun toByteSink(): ByteSink

  open fun getPayloadSize(): Int {
    return sizeof(packetVersion)
  }

  fun buildPacket(id: Long): Packet {
    val totalBodySize = (Packet.PACKET_BODY_SIZE + getPayloadSize())
    if (totalBodySize > Int.MAX_VALUE) {
      throw RuntimeException("bodySize exceeds Int.MAX_VALUE: $totalBodySize")
    }

    val packetBody = Packet.PacketBody(
      id,
      getPacketType().value,
      toByteSink()
    )

    return Packet(
      Packet.MAGIC_NUMBER,
      totalBodySize,
      packetBody
    )
  }
}