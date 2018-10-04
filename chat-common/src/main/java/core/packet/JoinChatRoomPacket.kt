package core.packet

import core.PacketType
import core.byte_sink.ByteSink
import core.byte_sink.InMemoryByteSink
import core.sizeof
import java.lang.IllegalStateException

class JoinChatRoomPacket(
  val ecPublicKey: ByteArray,
  val userName: String,
  val roomName: String,
  val roomPasswordHash: String?
) : BasePacket() {

  override val packetVersion: Short
    get() = CURRENT_PACKET_VERSION.value

  override fun getPacketType(): PacketType = PacketType.JoinRoomPacketType

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + sizeof(ecPublicKey) + sizeof(userName) + sizeof(roomName) + sizeof(roomPasswordHash)
  }

  override fun toByteSink(): ByteSink {
    return InMemoryByteSink.createWithInitialSize(getPayloadSize()).apply {
      writeShort(packetVersion)

      when (CURRENT_PACKET_VERSION) {
        JoinChatRoomPacket.PacketVersion.V1 -> {
          writeByteArray(ecPublicKey)
          writeString(userName)
          writeString(roomName)
          writeString(roomPasswordHash)
        }
        JoinChatRoomPacket.PacketVersion.Unknown -> throw IllegalStateException("Should not happen")
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
  }
}