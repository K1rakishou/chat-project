package core

import core.byte_sink.ByteSink

class PacketInfo(
  val packetId: Long,
  val packetType: PacketType,
  val byteSink: ByteSink
)