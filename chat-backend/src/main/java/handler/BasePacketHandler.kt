package handler

import core.Connection
import core.packet.IPacketPayload

abstract class BasePacketHandler {
  abstract suspend fun handle(packetId: Long, packetVersion: Int, packetPayload: IPacketPayload, connection: Connection)
}