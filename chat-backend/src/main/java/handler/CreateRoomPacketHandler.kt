package handler

import core.Status
import core.packet.CreateRoomPacketPayload
import core.response.BaseResponse
import core.response.ChatRoomCreatedResponse
import manager.ChatRoomManager
import manager.ConnectionManager

class CreateRoomPacketHandler(
  private val connectionManager: ConnectionManager,
  private val chatRoomManager: ChatRoomManager
) : BasePacketHandler() {

  override suspend fun handle(packetId: Long, packetPayloadRaw: ByteArray, clientAddress: String) {
    val packet = CreateRoomPacketPayload.fromByteArray(packetPayloadRaw)
    val packetVersion = CreateRoomPacketPayload.PacketVersion.fromShort(packet.packetVersion)

    val response = when (packetVersion) {
      CreateRoomPacketPayload.PacketVersion.V1 -> handleInternalV1(packet)
    }

    connectionManager.send(clientAddress, response)
  }

  private suspend fun handleInternalV1(packet: CreateRoomPacketPayload): BaseResponse {
    if (chatRoomManager.exists(packet.chatRoomName)) {
      println("ChatRoom with name ${packet.chatRoomName} already exists")
      return ChatRoomCreatedResponse(Status.ChatRoomWithThisNameAlreadyExists, null)
    }

    val chatRoom = chatRoomManager.createChatRoom(
      isPublic = packet.isPublic,
      chatRoomName = packet.chatRoomName,
      chatRoomPasswordHash = packet.chatRoomPasswordHash
    )

    println("ChatRoom ${chatRoom} has been successfully created!")
    return ChatRoomCreatedResponse(Status.Ok, chatRoom.roomName)
  }

}