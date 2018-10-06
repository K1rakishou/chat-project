package handler

import core.Status
import core.byte_sink.ByteSink
import core.exception.UnknownPacketVersion
import core.packet.CreateRoomPacket
import core.response.BaseResponse
import core.response.CreateRoomResponsePayload
import manager.ChatRoomManager
import manager.ConnectionManager

class CreateRoomPacketHandler(
  private val connectionManager: ConnectionManager,
  private val chatRoomManager: ChatRoomManager
) : BasePacketHandler() {

  override suspend fun handle(packetId: Long, byteSink: ByteSink, clientAddress: String) {
    val packetVersion = CreateRoomPacket.PacketVersion.fromShort(byteSink.readShort())

    val response = when (packetVersion) {
      CreateRoomPacket.PacketVersion.V1 -> handleInternalV1(byteSink)
      CreateRoomPacket.PacketVersion.Unknown -> throw UnknownPacketVersion()
    }

    connectionManager.sendResponse(clientAddress, response)
  }

  private suspend fun handleInternalV1(byteSink: ByteSink): BaseResponse {
    val isPublic = byteSink.readBoolean()
    val chatRoomName = byteSink.readString()
    val chatRoomPasswordHash = byteSink.readString()

    if (chatRoomManager.exists(chatRoomName)) {
      println("ChatRoom with name $chatRoomName already exists")
      return CreateRoomResponsePayload.fail(Status.ChatRoomAlreadyExists)
    }

    val chatRoom = chatRoomManager.createChatRoom(
      isPublic = isPublic,
      chatRoomName = chatRoomName,
      chatRoomPasswordHash = chatRoomPasswordHash
    )

    println("ChatRoom ${chatRoom} has been successfully created!")
    return CreateRoomResponsePayload.success(chatRoom.roomName)
  }

}