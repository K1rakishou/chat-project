package core.packet

import core.Packet
import core.byte_sink.ByteSink
import core.byte_sink.InMemoryByteSink

object PacketBuilder {

  fun buildPacket(packet: BasePacket, byteSink: ByteSink = InMemoryByteSink.createWithInitialSize(packet.getPayloadSize())): Packet {
    if (packet is UnencryptedPacket) {
      return buildUnencryptedPacket(packet, byteSink)
    } else {
      return buildEncryptedPacket(packet, byteSink)
    }
  }

  private fun buildUnencryptedPacket(packet: UnencryptedPacket, byteSink: ByteSink): Packet {
    val totalBodySize = (Packet.PACKET_BODY_SIZE + packet.getPayloadSize())
    if (totalBodySize > Int.MAX_VALUE) {
      throw RuntimeException("bodySize exceeds Int.MAX_VALUE: $totalBodySize")
    }

    packet.toByteSink(byteSink)

    val packetBody = Packet.PacketBody(
      //TODO: REMOVE
      -1L,
      packet.getPacketType().value,
      byteSink
    )

    return Packet(
      Packet.MAGIC_NUMBER,
      totalBodySize,
      packetBody
    )
  }

  private fun buildEncryptedPacket(packet: BasePacket, byteSink: ByteSink): Packet {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}