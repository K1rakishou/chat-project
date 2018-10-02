package core

import core.byte_sink.ByteSink
import core.packet.PacketType

class PacketInfo(
  val packetId: Long,
  val packetType: PacketType,
  val byteSink: ByteSink
)