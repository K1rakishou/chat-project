package handler

import core.Status
import core.byte_sink.ByteSink
import core.packet.GetPageOfPublicRoomsPacketPayload
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
    val packet = GetPageOfPublicRoomsPacketPayload.fromByteSink(byteSink)
    val packetVersion = GetPageOfPublicRoomsPacketPayload.PacketVersion.fromShort(packet.packetVersion)

    val response = when (packetVersion) {
      GetPageOfPublicRoomsPacketPayload.PacketVersion.V1 -> handleInternalV1(packet)
    }

    connectionManager.send(clientAddress, response)
  }

  private suspend fun handleInternalV1(packet: GetPageOfPublicRoomsPacketPayload): BaseResponse {
    val roomsPerPage = packet.roomsPerPage
      .toInt()
      .coerceIn(minimumRoomsPerPage, maximumRoomsPerPage)

    val skip = packet.currentPage.toInt() * roomsPerPage

    //TODO: speed this process up by using H2 in-memory database instead of java collections to store chatRooms
    val roomsPage = chatRoomManager.getAllPublicRooms()
      .asSequence()
      .drop(skip)
      .take(roomsPerPage)
      .toList()

    return GetPageOfPublicRoomsResponsePayload(Status.Ok, roomsPage)
  }

}