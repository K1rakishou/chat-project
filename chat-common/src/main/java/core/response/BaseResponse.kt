package core.response

import core.byte_sink.InMemoryByteSink
import core.Status
import core.packet.Packet
import core.sizeof

abstract class BaseResponse(
  val status: Status
) {
  abstract val packetVersion: Short
  abstract fun toByteSink(byteSink: InMemoryByteSink)

  open fun getPayloadSize(): Int {
    return sizeof(packetVersion)
  }

  fun responseToBytes(id: Long): ByteArray {
    val totalBodySize = (Packet.PACKET_BODY_SIZE + getPayloadSize())
    if (totalBodySize > Int.MAX_VALUE) {
      throw RuntimeException("bodySize exceeds Int.MAX_VALUE: $totalBodySize")
    }

    val byteSink = InMemoryByteSink.createWithInitialSize(totalBodySize)
    toByteSink(byteSink)

    val packetBody = Packet.PacketBody(
      id,
      packetVersion,
      byteSink.getArray()
    ).toByteBuffer().array()

    return Packet(
      Packet.MAGIC_NUMBER,
      totalBodySize,
      packetBody
    ).toByteArray()
  }
}