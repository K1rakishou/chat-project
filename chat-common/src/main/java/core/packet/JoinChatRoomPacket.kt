package core.packet

import core.Constants
import core.PacketType
import core.byte_sink.ByteSink
import core.exception.*
import core.sizeof

class JoinChatRoomPacket(
  val userName: String,
  val roomName: String,
  val roomPasswordHash: String?
) : BasePacket() {

  override fun getPacketVersion(): Short = CURRENT_PACKET_VERSION.value
  override fun getPacketType(): PacketType = PacketType.JoinRoomPacketType

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + sizeof(userName) + sizeof(roomName) + sizeof(roomPasswordHash)
  }

  override fun toByteSink(byteSink: ByteSink) {
    super.toByteSink(byteSink)

    when (CURRENT_PACKET_VERSION) {
      PacketVersion.V1 -> {
        byteSink.writeString(userName)
        byteSink.writeString(roomName)
        byteSink.writeString(roomPasswordHash)
      }
      PacketVersion.Unknown -> throw UnknownPacketVersionException(getPacketVersion())
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

    @Throws(PacketDeserializationException::class)
    fun fromByteSink(byteSink: ByteSink): JoinChatRoomPacket {
      try {
        val packetVersion = PacketVersion.fromShort(byteSink.readShort())
        when (packetVersion) {
          JoinChatRoomPacket.PacketVersion.V1 -> {
            val userName = byteSink.readString(Constants.maxUserNameLen)
              ?: throw PacketDeserializationException("Could not read userName")
            val roomName = byteSink.readString(Constants.maxChatRoomNameLen)
              ?: throw PacketDeserializationException("Could not read chatRoomName")
            val roomPasswordHash = byteSink.readString(Constants.maxChatRoomPasswordHashLen)

            return JoinChatRoomPacket(userName, roomName, roomPasswordHash)
          }
          JoinChatRoomPacket.PacketVersion.Unknown -> throw UnknownPacketVersionException(packetVersion.value)
        }
      } catch (error: Throwable) {
        when (error) {
          is ByteSinkBufferOverflowException,
          is ReaderPositionExceededBufferSizeException,
          is MaxListSizeExceededException,
          is UnknownPacketVersionException,
          is DrainableDeserializationException -> {
            throw PacketDeserializationException(error.message ?: "No exception message")
          }
          else -> throw error
        }
      }
    }
  }
}