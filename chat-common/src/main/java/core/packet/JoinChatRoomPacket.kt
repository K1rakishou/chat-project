package core.packet

import core.Constants
import core.PacketType
import core.byte_sink.ByteSink
import core.byte_sink.InMemoryByteSink
import core.exception.PacketDeserializationException
import core.exception.UnknownPacketVersionException
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

  override fun toByteSink(byteSink: ByteSink) {
    byteSink.writeShort(packetVersion)

    when (CURRENT_PACKET_VERSION) {
      JoinChatRoomPacket.PacketVersion.V1 -> {
        byteSink.writeByteArray(ecPublicKey)
        byteSink.writeString(userName)
        byteSink.writeString(roomName)
        byteSink.writeString(roomPasswordHash)
      }
      JoinChatRoomPacket.PacketVersion.Unknown -> throw UnknownPacketVersionException(packetVersion)
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
          val ecPublicKey = byteSink.readByteArray(Constants.maxEcPublicKeySize)
            ?: throw PacketDeserializationException("Could not read ecPublicKey")
          val userName = byteSink.readString(Constants.maxUserNameLen)
            ?: throw PacketDeserializationException("Could not read userName")
          val roomName = byteSink.readString(Constants.maxChatRoomNameLength)
            ?: throw PacketDeserializationException("Could not read chatRoomName")
          val roomPasswordHash = byteSink.readString(Constants.maxChatRoomPasswordHash)

          return JoinChatRoomPacket(ecPublicKey, userName, roomName, roomPasswordHash)
        }
        JoinChatRoomPacket.PacketVersion.Unknown -> throw UnknownPacketVersionException(packetVersion.value)
      }
    }
  }
}