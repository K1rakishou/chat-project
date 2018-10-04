package core.packet

import core.PacketType
import core.byte_sink.ByteSink
import core.byte_sink.InMemoryByteSink
import core.sizeof

class CreateRoomPacket(
  val isPublic: Boolean,
  val chatRoomName: String?,
  val chatRoomPasswordHash: String?
) : BasePacket() {

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
        CreateRoomPacket.PacketVersion.V1 -> {
          writeBoolean(isPublic)
          writeString(chatRoomName)
          writeString(chatRoomPasswordHash)
        }
        CreateRoomPacket.PacketVersion.Unknown -> throw IllegalStateException("Should not happen")
      }
    }
  }

  enum class PacketVersion(val value: Short) {
    Unknown(-1),
    V1(1);

    companion object {
      fun fromShort(value: Short): PacketVersion {
        return PacketVersion.values().firstOrNull { it.value == value } ?: Unknown
      }
    }
  }

  companion object {
    private val CURRENT_PACKET_VERSION = PacketVersion.V1

    fun fromByteSink(byteSink: ByteSink): CreateRoomPacket {
      val packetVersion = PacketVersion.fromShort(byteSink.readShort())

      when (packetVersion) {
        CreateRoomPacket.PacketVersion.V1 -> {
          val isPublic = byteSink.readBoolean()
          val chatRoomName = byteSink.readString()
          val chatRoomPasswordHash = byteSink.readString()

          return CreateRoomPacket(isPublic, chatRoomName, chatRoomPasswordHash)
        }
        CreateRoomPacket.PacketVersion.Unknown -> throw IllegalStateException("Should not happen")
      }
    }
  }
}