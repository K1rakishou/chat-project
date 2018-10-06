package core.packet

import core.PacketType
import core.byte_sink.ByteSink
import core.byte_sink.InMemoryByteSink
import core.exception.PacketDeserializationException
import core.exception.UnknownPacketVersion
import core.sizeof

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

    fun fromByteSink(byteSink: ByteSink): JoinChatRoomPacket {
      val packetVersion = PacketVersion.fromShort(byteSink.readShort())

      when (packetVersion) {
        JoinChatRoomPacket.PacketVersion.V1 -> {
          val ecPublicKey = byteSink.readByteArray() ?: throw PacketDeserializationException("Could not read ecPublicKey")
          val userName = byteSink.readString() ?: throw PacketDeserializationException("Could not read userName")
          val roomName = byteSink.readString() ?: throw PacketDeserializationException("Could not read roomName")
          val roomPasswordHash = byteSink.readString()

          return JoinChatRoomPacket(ecPublicKey, userName, roomName, roomPasswordHash)
        }
        JoinChatRoomPacket.PacketVersion.Unknown -> throw UnknownPacketVersion()
      }
    }
  }
}