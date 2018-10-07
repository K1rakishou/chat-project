package core.packet

import core.Packet
import core.PacketType
import core.byte_sink.ByteSink
import core.byte_sink.InMemoryByteSink
import core.sizeof

abstract class BasePacket {
  abstract val packetVersion: Short
  abstract fun getPacketType(): PacketType
  abstract fun toByteSink(byteSink: ByteSink)

  open fun getPayloadSize(): Int {
    return sizeof(packetVersion)
  }

  fun buildPacket(id: Long, byteSink: ByteSink = InMemoryByteSink.createWithInitialSize(getPayloadSize())): Packet {
    val totalBodySize = (Packet.PACKET_BODY_SIZE + getPayloadSize())
    if (totalBodySize > Int.MAX_VALUE) {
      throw RuntimeException("bodySize exceeds Int.MAX_VALUE: $totalBodySize")
    }

    toByteSink(byteSink)

    val packetBody = Packet.PacketBody(
      id,
      getPacketType().value,
      byteSink
    )

    return Packet(
      Packet.MAGIC_NUMBER,
      totalBodySize,
      packetBody
    )
  }
}