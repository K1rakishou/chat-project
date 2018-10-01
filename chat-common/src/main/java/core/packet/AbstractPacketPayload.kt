package core.packet

import core.byte_sink.InMemoryByteSink
import core.sizeof

abstract class AbstractPacketPayload {
  abstract val packetVersion: Short
  abstract fun getPacketType(): PacketType
  abstract fun toByteSink(): InMemoryByteSink

  open fun getPayloadSize(): Int {
    return sizeof(packetVersion)
  }

  fun packetToBytes(id: Long): ByteArray {
    val totalBodySize = (Packet.PACKET_BODY_SIZE + getPayloadSize())
    if (totalBodySize > Int.MAX_VALUE) {
      throw RuntimeException("bodySize exceeds Int.MAX_VALUE: $totalBodySize")
    }

    val packetBody = Packet.PacketBody(
      id,
      getPacketType().value,
      toByteSink().getArray()
    ).toByteBuffer().array()

    return Packet(
      Packet.MAGIC_NUMBER,
      totalBodySize,
      packetBody
    ).toByteArray()
  }
}