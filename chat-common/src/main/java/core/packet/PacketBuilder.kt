package core.packet

import core.Packet
import core.byte_sink.ByteSink
import java.lang.RuntimeException

class PacketBuilder {

  fun buildPacket(packet: BasePacket, byteSink: ByteSink): Packet {
    val payloadSize = packet.getPayloadSize()
    if (payloadSize > Int.MAX_VALUE) {
      throw RuntimeException("payloadSize exceeds Int.MAX_VALUE: $payloadSize")
    }

    packet.toByteSink(byteSink)

    if (payloadSize != byteSink.getWriterPosition()) {
      throw RuntimeException("payloadSize ($payloadSize) != byteSink.getWriterPosition() (${byteSink.getWriterPosition()})")
    }

    return Packet(
      Packet.MAGIC_NUMBER,
      payloadSize,
      packet.getPacketType().value,
      byteSink
    )
  }

}