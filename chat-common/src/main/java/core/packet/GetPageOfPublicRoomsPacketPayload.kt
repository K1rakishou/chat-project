package core.packet

import core.PositionAwareByteArray
import core.sizeof

class GetPageOfPublicRoomsPacketPayload(
  val currentPage: Short,
  val roomsPerPage: Byte
) : AbstractPacketPayload() {

  override val packetVersion: Short
    get() = CURRENT_PACKET_VERSION.value

  override fun getPacketType(): PacketType = PacketType.GetPageOfPublicRoomsPacketType

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + sizeof(currentPage) + sizeof(roomsPerPage)
  }

  override fun toByteArray(): PositionAwareByteArray {
    return PositionAwareByteArray.createWithInitialSize(getPayloadSize()).apply {
      writeShort(packetVersion)

      when (CURRENT_PACKET_VERSION) {
        GetPageOfPublicRoomsPacketPayload.PacketVersion.V1 -> {
          writeShort(currentPage)
          writeByte(roomsPerPage)
        }
      }
    }
  }

  enum class PacketVersion(val value: Short) {
    V1(1);

    companion object {
      fun fromShort(value: Short): PacketVersion {
        return PacketVersion.values().first { it.value == value }
      }
    }
  }

  companion object {
    private val CURRENT_PACKET_VERSION = PacketVersion.V1

    fun fromByteArray(array: ByteArray): GetPageOfPublicRoomsPacketPayload {
      val byteArray = PositionAwareByteArray.fromArray(array)
      val packetVersion = PacketVersion.fromShort(byteArray.readShort())

      return when (packetVersion) {
        GetPageOfPublicRoomsPacketPayload.PacketVersion.V1 -> {
          val currentPage = byteArray.readShort()
          val roomsPerPage = byteArray.readByte()

          GetPageOfPublicRoomsPacketPayload(currentPage, roomsPerPage)
        }
      }
    }
  }
}