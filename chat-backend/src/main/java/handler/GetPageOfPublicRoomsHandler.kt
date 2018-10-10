package handler

import core.Status
import core.byte_sink.ByteSink
import core.exception.PacketDeserializationException
import core.exception.UnknownPacketVersionException
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

  override suspend fun handle(byteSink: ByteSink, clientAddress: String) {
    val packet = try {
      GetPageOfPublicRoomsPacket.fromByteSink(byteSink)
    } catch (error: PacketDeserializationException) {
      error.printStackTrace()
      connectionManager.sendResponse(clientAddress, GetPageOfPublicRoomsResponsePayload.fail(Status.BadPacket))
      return
    }

    val packetVersion = GetPageOfPublicRoomsPacket.PacketVersion.fromShort(packet.packetVersion)

    val response = when (packetVersion) {
      GetPageOfPublicRoomsPacket.PacketVersion.V1 -> handleInternalV1(packet)
      GetPageOfPublicRoomsPacket.PacketVersion.Unknown -> throw UnknownPacketVersionException(packetVersion.value)
    }

    connectionManager.sendResponse(clientAddress, response)
  }

  private suspend fun handleInternalV1(packet: GetPageOfPublicRoomsPacket): BaseResponse {
    val currentPage = packet.currentPage
    val roomsPerPage = packet.roomsPerPage

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