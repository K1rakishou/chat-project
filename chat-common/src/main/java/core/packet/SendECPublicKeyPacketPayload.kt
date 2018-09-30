package core.packet

import core.PositionAwareByteArray
import core.sizeof

class SendECPublicKeyPacketPayload(
  private val ecPublicKey: ByteArray
) : AbstractPacketPayload() {

  override val packetVersion: Short
    get() = CURRENT_PACKET_VERSION.value

  override fun getPacketType(): PacketType = PacketType.SendECPublicKeyPacketType

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + sizeof(ecPublicKey)
  }

  override fun toByteArray(): PositionAwareByteArray {
    return PositionAwareByteArray.createWithInitialSize(getPayloadSize()).apply {
      writeShort(packetVersion)

      when (CURRENT_PACKET_VERSION) {
        SendECPublicKeyPacketPayload.PacketVersion.V1 -> {
          serializePacketV1(this)
        }
      }
    }
  }

  private fun serializePacketV1(byteArray: PositionAwareByteArray) {
    byteArray.apply {
      writeByteArray(ecPublicKey)
    }
  }

  enum class PacketVersion(val value: Short) {
    V1(1);

    companion object {
      fun fromShort(value: Short): PacketVersion {
        return PacketVersion.values().first { it.value == value }
      }
    }
  }

  companion object {
    private val CURRENT_PACKET_VERSION = PacketVersion.V1

    fun fromByteArray(array: ByteArray): SendECPublicKeyPacketPayload {
      val byteArray = PositionAwareByteArray.fromArray(array)
      val packetVersion = PacketVersion.fromShort(byteArray.readShort())

      return when (packetVersion) {
        SendECPublicKeyPacketPayload.PacketVersion.V1 -> {
          SendECPublicKeyPacketPayload(byteArray.readByteArray())
        }
      }
    }
  }
}