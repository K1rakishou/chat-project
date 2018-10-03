package core.response

import core.byte_sink.InMemoryByteSink
import core.Status
import core.byte_sink.ByteSink
import core.Packet
import core.sizeof

abstract class BaseResponse(
  val status: Status
) {
  abstract val packetType: Short
  abstract fun toByteSink(byteSink: ByteSink)

  open fun getPayloadSize(): Int {
    return sizeof(packetType) + sizeof(status)
  }

  fun buildResponse(id: Long): Packet {
    val totalBodySize = (Packet.PACKET_BODY_SIZE + getPayloadSize())
    if (totalBodySize > Int.MAX_VALUE) {
      throw RuntimeException("bodySize exceeds Int.MAX_VALUE: $totalBodySize")
    }

    val byteSink = InMemoryByteSink.createWithInitialSize(totalBodySize)
    toByteSink(byteSink)

    val packetBody = Packet.PacketBody(
      id,
      packetType,
      byteSink
    )

    return Packet(
      Packet.MAGIC_NUMBER,
      totalBodySize,
      packetBody
    )
  }
}