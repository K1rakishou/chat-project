package core.packet

import java.nio.ByteBuffer

class Packet(
  val magicNumber: Int,       //4
  val bodySize: Int,          //4

  val packetBody: ByteArray
) {

  fun getPacketHeaderSize(): Int {
    return PACKET_HEADER_SIZE
  }

  fun getPacketFullSize(): Int {
    return getPacketHeaderSize() + packetBody.size
  }

  fun toByteArray(): ByteArray {
    val packetBuffer = ByteBuffer.allocate(getPacketFullSize())

    packetBuffer.putInt(magicNumber)
    packetBuffer.putInt(bodySize)
    packetBuffer.put(packetBody)

    return packetBuffer.array()
  }

  class PacketBody(
    val id: Long,             //8
    val type: Short,          //2
    val data: ByteArray
  ) {

    fun getSize(): Int {
      return PACKET_BODY_SIZE + data.size
    }

    fun toByteBuffer(): ByteBuffer {
      val buffer = ByteBuffer.allocate(getSize())

      buffer.putLong(id)
      buffer.putShort(type)
      buffer.put(data)

      return buffer
    }
  }

  companion object {
    const val MAGIC_NUMBER = 0x44455355
    const val PACKET_HEADER_SIZE = 4 + 4
    const val PACKET_BODY_SIZE = 8 + 2
  }
}