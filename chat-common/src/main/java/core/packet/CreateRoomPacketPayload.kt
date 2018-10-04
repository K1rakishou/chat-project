package core.packet

import core.PacketType
import core.byte_sink.InMemoryByteSink
import core.sizeof

class CreateRoomPacketPayload(
  val isPublic: Boolean,
  val chatRoomName: String?,
  val chatRoomPasswordHash: String?
) : AbstractPacketPayload() {

  override val packetVersion: Short
    get() = CURRENT_PACKET_VERSION.value

  override fun getPacketType(): PacketType = PacketType.CreateRoomPacketType

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + sizeof(isPublic) + sizeof(chatRoomName) + sizeof(chatRoomPasswordHash)
  }

  override fun toByteSink(): InMemoryByteSink {
    return InMemoryByteSink.createWithInitialSize(getPayloadSize()).apply {
      writeShort(packetVersion)

      when (CURRENT_PACKET_VERSION) {
        CreateRoomPacketPayload.PacketVersion.V1 -> {
          writeBoolean(isPublic)
          writeString(chatRoomName)
          writeString(chatRoomPasswordHash)
        }
        CreateRoomPacketPayload.PacketVersion.Unknown -> throw IllegalStateException("Should not happen")
      }
    }
  }

  enum class PacketVersion(val value: Short) {
    Unknown(-1),
    V1(1);

    companion object {
      fun fromShort(value: Short): PacketVersion {
        return PacketVersion.values().first { it.value == value }
      }
    }
  }

  companion object {
    private val CURRENT_PACKET_VERSION = PacketVersion.V1
  }
}