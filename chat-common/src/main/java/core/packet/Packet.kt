package core.packet

import java.lang.IllegalArgumentException
import java.nio.ByteBuffer

class Packet(
  val magicNumber: Int,       //4
  val bodySize: Short,        //2

  val packetBody: PacketBody
) {

  fun getPacketHeaderSize(): Int {
    return PACKET_HEADER_SIZE
  }

  fun getPacketFullSize(): Int {
    return getPacketHeaderSize() + packetBody.getSize()
  }

  fun toByteArray(): ByteArray {
    val bodyArray = packetBody.toByteBuffer().array()
    val packetBuffer = ByteBuffer.allocate(getPacketFullSize())

    packetBuffer.putInt(magicNumber)
    packetBuffer.putShort(bodySize)
    packetBuffer.put(bodyArray)

    return packetBuffer.array()
  }

  class PacketBody(
    val id: Long,                 //8
    val version: Int,             //4
    val type: PacketType,         //2
    private val random1: Long,    //8
    private val random2: Long,    //8
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
      buffer.putLong(random1)
      buffer.putLong(random2)
      buffer.put(data)

      return buffer
    }
  }

  companion object {
    const val MAGIC_NUMBER = 0x69691911
    const val PACKET_HEADER_SIZE = 4 + 2
    const val PACKET_BODY_SIZE = 8 + 4 + 2 + 8 + 8
  }
}

enum class PacketType(val value: Short) {
  TestPacket(0);

  companion object {
    fun fromShort(type: Short): PacketType {
      return PacketType.values().firstOrNull { it.value == type }
        ?: throw IllegalArgumentException("Unknown packetType: $type")
    }
  }
}