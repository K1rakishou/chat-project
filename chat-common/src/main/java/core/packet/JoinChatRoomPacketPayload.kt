package core.packet

import core.PacketType
import core.byte_sink.ByteSink
import core.byte_sink.InMemoryByteSink
import core.sizeof

class JoinChatRoomPacketPayload(
  val ecPublicKey: ByteArray,
  val userName: String,
  val roomName: String,
  val roomPasswordHash: String?
) : AbstractPacketPayload() {

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
        JoinChatRoomPacketPayload.PacketVersion.V1 -> {
          writeByteArray(ecPublicKey)
          writeString(userName)
          writeString(roomName)
          writeString(roomPasswordHash)
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

    fun fromByteSink(byteSink: ByteSink): JoinChatRoomPacketPayload {
      val packetVersion = PacketVersion.fromShort(byteSink.readShort())

      return when (packetVersion) {
        JoinChatRoomPacketPayload.PacketVersion.V1 -> {
          val ecPublicKey = byteSink.readByteArray()
          val userName = byteSink.readString()!!
          val roomName = byteSink.readString()!!
          val roomPasswordHash = byteSink.readString()

          JoinChatRoomPacketPayload(ecPublicKey, userName, roomName, roomPasswordHash)
        }
      }
    }
  }
}