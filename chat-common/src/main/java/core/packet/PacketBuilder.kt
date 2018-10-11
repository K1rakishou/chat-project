package core.packet

import core.Packet
import core.byte_sink.ByteSink

class PacketBuilder {

  fun buildPacket(packet: UnencryptedPacket, byteSink: ByteSink): Packet? {
    val totalBodySize = (Packet.PACKET_BODY_SIZE + packet.getPayloadSize())
    if (totalBodySize > Int.MAX_VALUE) {
      println("bodySize exceeds Int.MAX_VALUE: $totalBodySize")
      return null
    }

    packet.toByteSink(byteSink)

    val packetBody = Packet.PacketBody(
      packet.getPacketType().value,
      byteSink
    )

    return Packet(
      Packet.MAGIC_NUMBER,
      totalBodySize,
      packetBody
    )
  }

}