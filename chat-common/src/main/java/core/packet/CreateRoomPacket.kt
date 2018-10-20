package core.packet

import core.Constants
import core.PacketType
import core.byte_sink.ByteSink
import core.exception.*
import core.sizeof

class CreateRoomPacket(
  val isPublic: Boolean,
  val chatRoomName: String?,
  val chatRoomPasswordHash: String?,
  val chatRoomImageUrl: String?,
  val userName: String?
) : BasePacket() {

  override fun getPacketVersion(): Short = CURRENT_PACKET_VERSION.value
  override fun getPacketType(): PacketType = PacketType.CreateRoomPacketType

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + sizeof(isPublic) + sizeof(chatRoomName) + sizeof(chatRoomPasswordHash) + sizeof(chatRoomImageUrl) + sizeof(userName)
  }

  override fun toByteSink(byteSink: ByteSink) {
    super.toByteSink(byteSink)

    when (CURRENT_PACKET_VERSION) {
      CreateRoomPacket.PacketVersion.V1 -> {
        byteSink.writeBoolean(isPublic)
        byteSink.writeString(chatRoomName)
        byteSink.writeString(chatRoomPasswordHash)
        byteSink.writeString(chatRoomImageUrl)
        byteSink.writeString(userName)
      }
      CreateRoomPacket.PacketVersion.Unknown -> throw UnknownPacketVersionException(getPacketVersion())
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
    fun fromByteSink(byteSink: ByteSink): CreateRoomPacket {
      try {
        val packetVersion = PacketVersion.fromShort(byteSink.readShort())
        when (packetVersion) {
          CreateRoomPacket.PacketVersion.V1 -> {
            val isPublic = byteSink.readBoolean()
            val chatRoomName = byteSink.readString(Constants.maxChatRoomNameLength)
            val chatRoomPasswordHash = byteSink.readString(Constants.maxChatRoomPasswordHashLen)
            val chatRoomImageUrl = byteSink.readString(Constants.maxImageUrlLen)
              ?: throw PacketDeserializationException("Could not read chatRoomImageUrl")
            val userName = byteSink.readString(Constants.maxUserNameLen)
              ?: throw PacketDeserializationException("Could not read userName")

            return CreateRoomPacket(isPublic, chatRoomName, chatRoomPasswordHash, chatRoomImageUrl, userName)
          }
          CreateRoomPacket.PacketVersion.Unknown -> throw UnknownPacketVersionException(packetVersion.value)
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