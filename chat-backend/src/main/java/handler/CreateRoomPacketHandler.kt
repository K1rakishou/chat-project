package handler

import core.Status
import core.byte_sink.ByteSink
import core.exception.PacketDeserializationException
import core.exception.UnknownPacketVersionException
import core.packet.CreateRoomPacket
import core.response.BaseResponse
import core.response.CreateRoomResponsePayload
import manager.ChatRoomManager
import manager.ConnectionManager

class CreateRoomPacketHandler(
  private val connectionManager: ConnectionManager,
  private val chatRoomManager: ChatRoomManager
) : BasePacketHandler() {

  override suspend fun handle(byteSink: ByteSink, clientAddress: String) {
    val packet = try {
      CreateRoomPacket.fromByteSink(byteSink)
    } catch (error: PacketDeserializationException) {
      error.printStackTrace()
      connectionManager.sendResponse(clientAddress, CreateRoomResponsePayload.fail(Status.BadPacket))
      return
    }

    val packetVersion = CreateRoomPacket.PacketVersion.fromShort(packet.getPacketVersion())
    val response = when (packetVersion) {
      CreateRoomPacket.PacketVersion.V1 -> handleInternalV1(packet)
      CreateRoomPacket.PacketVersion.Unknown -> throw UnknownPacketVersionException(packetVersion.value)
    }

    connectionManager.sendResponse(clientAddress, response)
  }

  private suspend fun handleInternalV1(packet: CreateRoomPacket): BaseResponse {
    if (chatRoomManager.exists(packet.chatRoomName)) {
      println("ChatRoom with name ${packet.chatRoomName} already exists")
      return CreateRoomResponsePayload.fail(Status.ChatRoomAlreadyExists)
    }

    val chatRoom = chatRoomManager.createChatRoom(
      isPublic = packet.isPublic,
      chatRoomName = packet.chatRoomName,
      chatRoomPasswordHash = packet.chatRoomPasswordHash
    )

    println("ChatRoom ${chatRoom} has been successfully created!")
    return CreateRoomResponsePayload.success(chatRoom.chatRoomName)
  }

}