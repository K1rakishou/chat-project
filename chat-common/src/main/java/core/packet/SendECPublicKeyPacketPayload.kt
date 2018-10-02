package core.packet

import core.byte_sink.ByteSink
import core.byte_sink.InMemoryByteSink
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

  override fun toByteSink(): InMemoryByteSink {
    return InMemoryByteSink.createWithInitialSize(getPayloadSize()).apply {
      writeShort(packetVersion)

      when (CURRENT_PACKET_VERSION) {
        SendECPublicKeyPacketPayload.PacketVersion.V1 -> {
          serializePacketV1(this)
        }
      }
    }
  }

  private fun serializePacketV1(byteSink: ByteSink) {
    byteSink.apply {
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

    fun fromByteSink(byteSink: ByteSink): SendECPublicKeyPacketPayload {
      val packetVersion = PacketVersion.fromShort(byteSink.readShort())

      return when (packetVersion) {
        SendECPublicKeyPacketPayload.PacketVersion.V1 -> {
          SendECPublicKeyPacketPayload(byteSink.readByteArray())
        }
      }
    }
  }
}