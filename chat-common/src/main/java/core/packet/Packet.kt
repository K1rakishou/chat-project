package core.packet

import java.lang.IllegalArgumentException
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
    val id: Long,                 //8
    val version: Int,             //4
    val type: PacketType,         //2
    val data: ByteArray
  ) {

    fun getSize(): Int {
      return PACKET_BODY_SIZE + data.size
    }

    fun toByteBuffer(): ByteBuffer {
      val buffer = ByteBuffer.allocate(getSize())

      buffer.putLong(id)
      buffer.putInt(version)
      buffer.putShort(type.value)
      buffer.put(data)

      return buffer
    }
  }

  companion object {
    const val MAGIC_NUMBER = 0x69691911
    const val PACKET_HEADER_SIZE = 4 + 4
    const val PACKET_BODY_SIZE = 8 + 4 + 2
  }
}

enum class PacketType(val value: Short) {
  SendECPublicKeyPacketPayload(0);

  companion object {
    fun fromShort(type: Short): PacketType {
      return PacketType.values().firstOrNull { it.value == type }
        ?: throw IllegalArgumentException("Unknown packetType: $type")
    }
  }
}