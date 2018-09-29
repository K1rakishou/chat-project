package core

import core.packet.IPacket
import core.packet.PacketType

class PacketInfo(
  val packetId: Long,
  val packetVersion: Int,
  val packetType: PacketType,
  val packet: IPacket
)