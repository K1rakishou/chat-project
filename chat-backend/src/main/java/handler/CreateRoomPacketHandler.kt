package handler

import core.Status
import core.User
import core.byte_sink.ByteSink
import core.exception.PacketDeserializationException
import core.exception.UnknownPacketVersionException
import core.packet.CreateRoomPacket
import core.response.BaseResponse
import core.response.CreateRoomResponsePayload
import core.utils.Validators
import manager.ChatRoomManager
import manager.ConnectionManager

class CreateRoomPacketHandler(
  private val connectionManager: ConnectionManager,
  private val chatRoomManager: ChatRoomManager
) : BasePacketHandler() {

  override suspend fun handle(byteSink: ByteSink, clientId: String) {
    val packet = try {
      CreateRoomPacket.fromByteSink(byteSink)
    } catch (error: PacketDeserializationException) {
      error.printStackTrace()
      connectionManager.sendResponse(clientId, CreateRoomResponsePayload.fail(Status.BadPacket))
      return
    }

    val packetVersion = CreateRoomPacket.PacketVersion.fromShort(packet.getPacketVersion())
    val response = when (packetVersion) {
      CreateRoomPacket.PacketVersion.V1 -> handleInternalV1(packet, clientId)
      CreateRoomPacket.PacketVersion.Unknown -> throw UnknownPacketVersionException(packetVersion.value)
    }

    connectionManager.sendResponse(clientId, response)
  }

  private suspend fun handleInternalV1(packet: CreateRoomPacket, clientId: String): BaseResponse {
    val chatRoomName = packet.chatRoomName!!
    val chatRoomImageUrl = packet.chatRoomImageUrl!!
    val chatRoomPasswordHash = packet.chatRoomPasswordHash
    val userName = packet.userName
    val isPublic = packet.isPublic

    val status = validateV1(chatRoomName, chatRoomImageUrl, chatRoomPasswordHash, userName)
    if (status != null) {
      return CreateRoomResponsePayload.fail(status)
    }

    if (chatRoomManager.exists(chatRoomName)) {
      println("ChatRoom with name $chatRoomName already exists")
      return CreateRoomResponsePayload.fail(Status.ChatRoomAlreadyExists)
    }

    val chatRoom = chatRoomManager.createChatRoom(
      isPublic = isPublic,
      chatRoomName = chatRoomName,
      chatRoomPasswordHash = chatRoomPasswordHash,
      chatRoomImageUrl = chatRoomImageUrl
    )

    println("ChatRoom ${chatRoom} has been successfully created!")

    if (userName != null) {
      val newUser = User(userName, clientId)
      val joinedChatRoom = chatRoomManager.joinRoom(clientId, chatRoomName, newUser)

      if (joinedChatRoom == null) {
        println("Could not join the room (${joinedChatRoom}) by user (${newUser.clientId}, ${newUser.userName})")
        return CreateRoomResponsePayload.fail(Status.CouldNotJoinChatRoom)
      }
    }

    return CreateRoomResponsePayload.success()
  }

  private fun validateV1(
    chatRoomName: String,
    chatRoomImageUrl: String,
    chatRoomPasswordHash: String?,
    userName: String?
  ): Status? {
    var status: Status? = null

    status = Validators.validateChatRoomName(chatRoomName)
    if (status != null) {
      return status
    }

    status = Validators.validateChatRoomPasswordHash(chatRoomPasswordHash)
    if (status != null) {
      return status
    }

    if (!Validators.isImageUrlValid(chatRoomImageUrl)) {
      println("chatRoomImageUrl is not valid")
      return Status.BadParam
    }

    status = Validators.validateUserName(userName)
    if (status != null) {
      return status
    }

    return null
  }
}