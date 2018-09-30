package core.response

import core.PositionAwareByteArray
import core.Status
import core.packet.Packet
import core.sizeof

abstract class BaseResponse(
  val status: Status
) {
  abstract val packetVersion: Short
  abstract fun toByteArray(byteArray: PositionAwareByteArray)

  open fun getPayloadSize(): Int {
    return sizeof(packetVersion)
  }

  fun responseToBytes(id: Long): ByteArray {
    val totalBodySize = (Packet.PACKET_BODY_SIZE + getPayloadSize())
    if (totalBodySize > Int.MAX_VALUE) {
      throw RuntimeException("bodySize exceeds Int.MAX_VALUE: $totalBodySize")
    }

    val byteArray = PositionAwareByteArray.createWithInitialSize(totalBodySize)
    toByteArray(byteArray)

    val packetBody = Packet.PacketBody(
      id,
      packetVersion,
      byteArray.getArray()
    ).toByteBuffer().array()

    return Packet(
      Packet.MAGIC_NUMBER,
      totalBodySize,
      packetBody
    ).toByteArray()
  }
}