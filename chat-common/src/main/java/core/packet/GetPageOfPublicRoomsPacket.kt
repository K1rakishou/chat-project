package core.packet

import core.PacketType
import core.byte_sink.ByteSink
import core.byte_sink.InMemoryByteSink
import core.exception.UnknownPacketVersion
import core.sizeof
import java.lang.IllegalStateException

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

  override fun toByteSink(): InMemoryByteSink {
    return InMemoryByteSink.createWithInitialSize(getPayloadSize()).apply {
      writeShort(packetVersion)

      when (CURRENT_PACKET_VERSION) {
        GetPageOfPublicRoomsPacket.PacketVersion.V1 -> {
          writeShort(currentPage)
          writeByte(roomsPerPage)
        }
        GetPageOfPublicRoomsPacket.PacketVersion.Unknown -> throw IllegalStateException("Should not happen")
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

    fun fromByteSink(byteSink: ByteSink): GetPageOfPublicRoomsPacket {
      val packetVersion = PacketVersion.fromShort(byteSink.readShort())

      when (packetVersion) {
        GetPageOfPublicRoomsPacket.PacketVersion.V1 -> {
          val currentPage = byteSink.readShort()
          val roomsPerPage = byteSink.readByte()

          return GetPageOfPublicRoomsPacket(currentPage, roomsPerPage)
        }
        GetPageOfPublicRoomsPacket.PacketVersion.Unknown -> throw UnknownPacketVersion()
      }
    }
  }
}