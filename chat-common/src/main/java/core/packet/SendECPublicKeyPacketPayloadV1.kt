package core.packet

import core.getInt
import core.sizeof
import java.nio.ByteBuffer

class SendECPublicKeyPacketPayloadV1(
  val ecPublicKey: ByteArray
) : IPacketPayload {

  override fun getPacketType(): PacketType = PacketType.SendECPublicKeyPacketPayload

  override fun getPacketVersion(): Int = PACKET_VERSION

  override fun getPayloadSize(): Int {
    return sizeof(ecPublicKey)
  }

  override fun toByteBuffer(): ByteBuffer {
    return ByteBuffer.allocate(sizeof(ecPublicKey)).apply {
      put(ecPublicKey)
    }
  }

  companion object {
    const val PACKET_VERSION = 1

    fun fromByteArray(byteArray: ByteArray): SendECPublicKeyPacketPayloadV1 {
      val keySize = byteArray.getInt(0)
      val key = byteArray.copyOfRange(4, 4 + keySize)

      return SendECPublicKeyPacketPayloadV1(key)
    }
  }
}