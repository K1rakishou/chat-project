package core.packet

import core.Constants
import core.PacketType
import core.byte_sink.ByteSink
import core.byte_sink.InMemoryByteSink
import core.exception.PacketDeserializationException
import core.exception.UnknownPacketVersionException
import core.sizeof

class SendChatMessagePacket(
  val messageId: Int,
  val roomName: String,
  val userName: String,
  val message: String
) : BasePacket() {

  override val packetVersion: Short
    get() = CURRENT_PACKET_VERSION.value

  override fun getPacketType(): PacketType = PacketType.SendChatMessagePacketType

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + sizeof(messageId) + sizeof(roomName) + sizeof(userName) + sizeof(message)
  }

  override fun toByteSink(byteSink: ByteSink) {
    byteSink.writeShort(packetVersion)

    when (CURRENT_PACKET_VERSION) {
      PacketVersion.V1 -> {
        byteSink.writeInt(messageId)
        byteSink.writeString(roomName)
        byteSink.writeString(userName)
        byteSink.writeString(message)
      }
      SendChatMessagePacket.PacketVersion.Unknown -> throw UnknownPacketVersionException(packetVersion)
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

    fun fromByteSink(byteSink: ByteSink): SendChatMessagePacket {
      val packetVersion = PacketVersion.fromShort(byteSink.readShort())

      when (packetVersion) {
        SendChatMessagePacket.PacketVersion.V1 -> {
          val messageId = byteSink.readInt()
          val roomName = byteSink.readString(Constants.maxChatRoomNameLength)
            ?: throw PacketDeserializationException("Could not read roomName")
          val userName = byteSink.readString(Constants.maxUserNameLen)
            ?: throw PacketDeserializationException("Could not read userName")
          val message = byteSink.readString(Constants.maxTextMessageLen)
            ?: throw PacketDeserializationException("Could not read message")

          return SendChatMessagePacket(messageId, roomName, userName, message)
        }
        SendChatMessagePacket.PacketVersion.Unknown -> throw UnknownPacketVersionException(packetVersion.value)
      }
    }
  }
}