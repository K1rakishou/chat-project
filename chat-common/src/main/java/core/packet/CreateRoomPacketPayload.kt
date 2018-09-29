package core.packet

import core.PositionAwareByteArray
import core.sizeof

class CreateRoomPacketPayload(
  val isPublic: Boolean,
  val chatRoomName: String?,
  val chatRoomPasswordHash: String?
) : AbstractPacketPayload() {

  override val packetVersion: Short
    get() = CURRENT_PACKET_VERSION.value

  override fun getPacketType(): PacketType = PacketType.CreateRoomPacketPayload

  override fun getPayloadSize(): Int {
    return sizeof(CURRENT_PACKET_VERSION.value) + sizeof(isPublic) + sizeof(chatRoomName) + sizeof(chatRoomPasswordHash)
  }

  override fun toByteArray(): PositionAwareByteArray {
    return PositionAwareByteArray.createWithInitialSize(getPayloadSize()).apply {
      writeShort(CURRENT_PACKET_VERSION.value)

      when (CURRENT_PACKET_VERSION) {
        CreateRoomPacketPayload.PacketVersion.V1 -> {
          serializePacketV1(this)
        }
      }
    }
  }

  private fun serializePacketV1(byteArray: PositionAwareByteArray) {
    byteArray.apply {
      writeBoolean(isPublic)
      writeString(chatRoomName)
      writeString(chatRoomPasswordHash)
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

    fun fromByteArray(array: ByteArray): CreateRoomPacketPayload {
      val byteArray = PositionAwareByteArray.fromArray(array)
      val packetVersion = PacketVersion.fromShort(byteArray.readShort())

      return when (packetVersion) {
        CreateRoomPacketPayload.PacketVersion.V1 -> {
          deserializePacketV1(byteArray)
        }
      }
    }

    private fun deserializePacketV1(byteArray: PositionAwareByteArray): CreateRoomPacketPayload {
      val isPublic = byteArray.readBoolean()
      val chatRoomName = byteArray.readString()
      val chatRoomPasswordHash = byteArray.readString()

      return CreateRoomPacketPayload(isPublic, chatRoomName, chatRoomPasswordHash)
    }
  }
}