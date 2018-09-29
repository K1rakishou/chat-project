package core

import core.packet.IPacketPayload
import core.packet.PacketType

class PacketInfo(
  val packetId: Long,
  val packetVersion: Int,
  val packetType: PacketType,
  val packetPayload: IPacketPayload
)