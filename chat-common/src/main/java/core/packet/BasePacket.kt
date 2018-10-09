package core.packet

import core.PacketType
import core.byte_sink.ByteSink
import core.sizeof

abstract class BasePacket {
  abstract val packetVersion: Short
  abstract fun getPacketType(): PacketType
  abstract fun toByteSink(byteSink: ByteSink)

  open fun getPayloadSize(): Int {
    return sizeof(packetVersion)
  }
}