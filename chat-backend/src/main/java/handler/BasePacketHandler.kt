package handler

import core.Connection
import core.packet.IPacket

abstract class BasePacketHandler {
  abstract suspend fun handle(packetId: Long, packetVersion: Int, packet: IPacket, connection: Connection)
}