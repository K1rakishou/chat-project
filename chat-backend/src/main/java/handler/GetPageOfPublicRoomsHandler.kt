package handler

import core.Status
import core.byte_sink.ByteSink
import core.exception.UnknownPacketVersion
import core.packet.GetPageOfPublicRoomsPacket
import core.response.BaseResponse
import core.response.GetPageOfPublicRoomsResponsePayload
import manager.ChatRoomManager
import manager.ConnectionManager

class GetPageOfPublicRoomsHandler(
  private val connectionManager: ConnectionManager,
  private val chatRoomManager: ChatRoomManager
) : BasePacketHandler() {

  private val minimumRoomsPerPage = 10
  private val maximumRoomsPerPage = 50

  override suspend fun handle(packetId: Long, byteSink: ByteSink, clientAddress: String) {
    val packetVersion = GetPageOfPublicRoomsPacket.PacketVersion.fromShort(byteSink.readShort())

    val response = when (packetVersion) {
      GetPageOfPublicRoomsPacket.PacketVersion.V1 -> handleInternalV1(byteSink)
      GetPageOfPublicRoomsPacket.PacketVersion.Unknown -> throw UnknownPacketVersion()
    }

    connectionManager.sendResponse(clientAddress, response)
  }

  private suspend fun handleInternalV1(byteSink: ByteSink): BaseResponse {
    val currentPage = byteSink.readShort()
    val roomsPerPage = byteSink.readByte()

    val count = roomsPerPage
      .toInt()
      .coerceIn(minimumRoomsPerPage, maximumRoomsPerPage)

    val skip = currentPage.toInt() * roomsPerPage

    //TODO: speed this process up by using H2 in-memory database instead of java collections to store chatRooms
    val roomsPage = chatRoomManager.getAllPublicRooms()
      .asSequence()
      .drop(skip)
      .take(count)
      .toList()

    return GetPageOfPublicRoomsResponsePayload.success(roomsPage)
  }

}