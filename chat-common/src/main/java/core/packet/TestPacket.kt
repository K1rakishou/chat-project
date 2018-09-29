package core.packet

import core.getInt
import core.getLong
import core.sizeof
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.charset.Charset

class TestPacket(
  val time: Long,
  val message: String
) : IPacket {

  fun toByteBuffer(): ByteBuffer {
    val byteBuffer = ByteBuffer.allocate(sizeof(time) + sizeof(message))

    byteBuffer.putLong(time)
    byteBuffer.putInt(message.length)
    byteBuffer.put(message.toByteArray(Charset.forName("UTF-8")))

    return byteBuffer
  }

  companion object {
    const val PACKET_VERSION = 1

    fun fromByteArray(byteArray: ByteArray): TestPacket {
      val time = byteArray.getLong(0)

      val stringSize = byteArray.getInt(8)
      if (stringSize >= Short.MAX_VALUE) {
        throw RuntimeException("String is way too long! stringSize = ${stringSize}")
      }

      val array = byteArray.copyOfRange(12, 12 + stringSize)
      val message = String(array)

      return TestPacket(time, message)
    }
  }
}