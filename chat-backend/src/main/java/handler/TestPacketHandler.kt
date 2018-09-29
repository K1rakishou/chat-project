package handler

import core.Connection
import core.packet.IPacketPayload
import manager.ConnectionManager

class TestPacketHandler(
  private val connectionManager: ConnectionManager
) : BasePacketHandler() {

  override suspend fun handle(packetId: Long, packetVersion: Int, packetPayload: IPacketPayload, connection: Connection) {
  }

}