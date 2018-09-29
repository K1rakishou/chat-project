package handler

import core.Connection
import core.packet.IPacket
import manager.ConnectionManager

class TestPacketHandler(
  private val connectionManager: ConnectionManager
) : BasePacketHandler() {

  override suspend fun handle(packetId: Long, packetVersion: Int, packet: IPacket, connection: Connection) {
  }

}