package core.packet

import core.PacketType
import core.byte_sink.ByteSink
import core.exception.*
import core.sizeof

class GetPageOfPublicRoomsPacket(
  val currentPage: Short,
  val roomsPerPage: Byte
) : BasePacket() {

  override val packetVersion: Short
    get() = CURRENT_PACKET_VERSION.value

  override fun getPacketType(): PacketType = PacketType.GetPageOfPublicRoomsPacketType

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + sizeof(currentPage) + sizeof(roomsPerPage)
  }

  override fun toByteSink(byteSink: ByteSink) {
    byteSink.writeShort(packetVersion)

    when (CURRENT_PACKET_VERSION) {
      GetPageOfPublicRoomsPacket.PacketVersion.V1 -> {
        byteSink.writeShort(currentPage)
        byteSink.writeByte(roomsPerPage)
      }
      GetPageOfPublicRoomsPacket.PacketVersion.Unknown -> throw UnknownPacketVersionException(packetVersion)
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
    fun fromByteSink(byteSink: ByteSink): GetPageOfPublicRoomsPacket {
      try {
        val packetVersion = PacketVersion.fromShort(byteSink.readShort())
        when (packetVersion) {
          GetPageOfPublicRoomsPacket.PacketVersion.V1 -> {
            val currentPage = byteSink.readShort()
            val roomsPerPage = byteSink.readByte()

            return GetPageOfPublicRoomsPacket(currentPage, roomsPerPage)
          }
          GetPageOfPublicRoomsPacket.PacketVersion.Unknown -> throw UnknownPacketVersionException(packetVersion.value)
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