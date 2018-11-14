package handler

import core.Constants
import core.Status
import core.byte_sink.ByteSink
import core.exception.PacketDeserializationException
import core.exception.UnknownPacketVersionException
import core.packet.SearchChatRoomPacket
import core.response.SearchChatRoomResponsePayload
import manager.ChatRoomManager
import manager.ConnectionManager

class SearchChatRoomPacketHandler(
  private val connectionManager: ConnectionManager,
  private val chatRoomManager: ChatRoomManager
) : BasePacketHandler() {

  override suspend fun handle(byteSink: ByteSink, clientId: String) {
    val packet = try {
      SearchChatRoomPacket.fromByteSink(byteSink)
    } catch (error: PacketDeserializationException) {
      error.printStackTrace()
      connectionManager.sendResponse(clientId, SearchChatRoomResponsePayload.fail(Status.BadPacket))
      return
    }

    val packetVersion = SearchChatRoomPacket.PacketVersion.fromShort(packet.getPacketVersion())
    when (packetVersion) {
      SearchChatRoomPacket.PacketVersion.V1 -> handleInternalV1(packet, clientId)
      SearchChatRoomPacket.PacketVersion.Unknown -> throw UnknownPacketVersionException(packetVersion.value)
    }
  }

  private suspend fun handleInternalV1(packet: SearchChatRoomPacket, clientId: String) {
    val chatRoomName = packet.chatRoomName

    //TODO: validate
    if (chatRoomName.isBlank()) {
      println("chatRoomName is blank")
      connectionManager.sendResponse(clientId, SearchChatRoomResponsePayload.fail(Status.BadParam))
      return
    }

    val foundRooms = chatRoomManager.searchForRoomsWithSimilarName(chatRoomName, Constants.maxFoundChatRooms)
    connectionManager.sendResponse(clientId, SearchChatRoomResponsePayload.success(foundRooms))

    println("Found ${foundRooms.size} chatRooms")
  }

}