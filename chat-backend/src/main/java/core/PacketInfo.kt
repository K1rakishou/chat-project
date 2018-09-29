package core

import core.packet.PacketType

class PacketInfo(
  val packetId: Long,
  val packetType: PacketType,
  val packetPayloadRaw: ByteArray
)