package handler

import core.Status
import core.byte_sink.ByteSink
import core.packet.CreateRoomPacketPayload
import core.response.BaseResponse
import core.response.CreateRoomResponsePayload
import manager.ChatRoomManager
import manager.ConnectionManager

class CreateRoomPacketHandler(
  private val connectionManager: ConnectionManager,
  private val chatRoomManager: ChatRoomManager
) : BasePacketHandler() {

  override suspend fun handle(packetId: Long, byteSink: ByteSink, clientAddress: String) {
    val packet = CreateRoomPacketPayload.fromByteSink(byteSink)
    val packetVersion = CreateRoomPacketPayload.PacketVersion.fromShort(packet.packetVersion)

    val response = when (packetVersion) {
      CreateRoomPacketPayload.PacketVersion.V1 -> handleInternalV1(packet)
    }

    connectionManager.send(clientAddress, response)
  }

  private suspend fun handleInternalV1(packet: CreateRoomPacketPayload): BaseResponse {
    if (chatRoomManager.exists(packet.chatRoomName)) {
      println("ChatRoom with name ${packet.chatRoomName} already exists")
      return CreateRoomResponsePayload(Status.ChatRoomAlreadyExists, null)
    }

    val chatRoom = chatRoomManager.createChatRoom(
      isPublic = packet.isPublic,
      chatRoomName = packet.chatRoomName,
      chatRoomPasswordHash = packet.chatRoomPasswordHash
    )

    println("ChatRoom ${chatRoom} has been successfully created!")
    return CreateRoomResponsePayload(Status.Ok, chatRoom.roomName)
  }

}