package core.packet

import core.PacketType
import core.byte_sink.ByteSink
import core.sizeof

abstract class BasePacket {
  abstract fun getPacketVersion(): Short
  abstract fun getPacketType(): PacketType

  open fun toByteSink(byteSink: ByteSink) {
    byteSink.writeShort(getPacketVersion())
  }

  open fun getPayloadSize(): Int {
    return sizeof(getPacketVersion())
  }
}