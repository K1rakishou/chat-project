package core

import core.byte_sink.ByteSink

class PacketInfo(
  val packetType: PacketType,
  val byteSink: ByteSink
)