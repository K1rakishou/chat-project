package core.packet

import core.Constants
import core.PacketType
import core.byte_sink.ByteSink
import core.exception.*
import core.sizeof

class SearchChatRoomPacket(
  val chatRoomName: String
) : BasePacket() {

  override fun getPacketVersion(): Short = CURRENT_PACKET_VERSION.value
  override fun getPacketType(): PacketType = PacketType.SearchChatRoomPacketType

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + sizeof(chatRoomName)
  }

  override fun toByteSink(byteSink: ByteSink) {
    super.toByteSink(byteSink)

    when (CURRENT_PACKET_VERSION) {
      PacketVersion.V1 -> {
        byteSink.writeString(chatRoomName)
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
    fun fromByteSink(byteSink: ByteSink): SearchChatRoomPacket {
      try {
        val packetVersion = PacketVersion.fromShort(byteSink.readShort())
        when (packetVersion) {
          PacketVersion.V1 -> {
            val chatRoomName = byteSink.readString(Constants.maxChatRoomNameLen)
              ?: throw PacketDeserializationException("Could not read chatRoomName")

            return SearchChatRoomPacket(chatRoomName)
          }
          PacketVersion.Unknown -> throw UnknownPacketVersionException(packetVersion.value)
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