package handler

import core.Constants
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
      CreateRoomPacket.PacketVersion.V1 -> handleInternalV1(packet, clientAddress)
      CreateRoomPacket.PacketVersion.Unknown -> throw UnknownPacketVersionException(packetVersion.value)
    }

    connectionManager.sendResponse(clientAddress, response)
  }

  private suspend fun handleInternalV1(packet: CreateRoomPacket, clientAddress: String): BaseResponse {
    val chatRoomName = packet.chatRoomName!!
    val chatRoomImageUrl = packet.chatRoomImageUrl!!
    val chatRoomPasswordHash = packet.chatRoomPasswordHash
    val userName = packet.userName
    val isPublic = packet.isPublic

    val response = validateV1(chatRoomName, chatRoomImageUrl, chatRoomPasswordHash, userName)
    if (response != null) {
      return response
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
      val newUser = User(userName, clientAddress)
      val joinedChatRoom = chatRoomManager.joinRoom(clientAddress, chatRoomName, newUser)

      if (joinedChatRoom == null) {
        println("Could not join the room (${joinedChatRoom}) by user (${newUser.clientAddress}, ${newUser.userName})")
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
  ): BaseResponse? {
    if (chatRoomName.isBlank()) {
      println("chatRoomName is blank: ${chatRoomName}")
      return CreateRoomResponsePayload.fail(Status.BadParam)
    }

    if (chatRoomImageUrl.isBlank()) {
      println("chatRoomImageUrl is blank: ${chatRoomImageUrl}")
      return CreateRoomResponsePayload.fail(Status.BadParam)
    }

    if (chatRoomName.length < Constants.minChatRoomNameLen) {
      println("chatRoomName.length (${chatRoomName.length}) < Constants.minChatRoomNameLen (${Constants.minChatRoomNameLen})")
      return CreateRoomResponsePayload.fail(Status.BadParam)
    }

    if (chatRoomName.length > Constants.maxChatRoomNameLength) {
      println("chatRoomName.length (${chatRoomName.length}) > Constants.maxChatRoomNameLength (${Constants.maxChatRoomNameLength})")
      return CreateRoomResponsePayload.fail(Status.BadParam)
    }

    if (chatRoomName.isBlank()) {
      println("chatRoomName is blank ($chatRoomName)")
      return CreateRoomResponsePayload.fail(Status.BadParam)
    }

    chatRoomPasswordHash?.let { roomPasswordHash ->
      if (roomPasswordHash.isBlank()) {
        println("chatRoomPasswordHash is blank ($chatRoomName)")
        return CreateRoomResponsePayload.fail(Status.BadParam)
      }

      if (roomPasswordHash.length < Constants.minChatRoomPasswordLen) {
        println("chatRoomPasswordHash (${roomPasswordHash.length}) < Constants.minChatRoomPasswordLen (${Constants.minChatRoomPasswordLen})")
        return CreateRoomResponsePayload.fail(Status.BadParam)
      }

      if (roomPasswordHash.length > Constants.maxChatRoomPasswordHashLen) {
        println("chatRoomPasswordHash (${roomPasswordHash.length}) > Constants.minChatRoomPasswordLen (${Constants.maxChatRoomPasswordHashLen})")
        return CreateRoomResponsePayload.fail(Status.BadParam)
      }
    }

    if (!Validators.isImageUrlValid(chatRoomImageUrl)) {
      println("chatRoomImageUrl is not valid")
      return CreateRoomResponsePayload.fail(Status.BadParam)
    }

    if (userName != null) {
      if (userName.isBlank()) {
        println("userName is blank ($chatRoomName)")
        return CreateRoomResponsePayload.fail(Status.BadParam)
      }

      if (userName.length < Constants.minUserNameLen) {
        println("userName.length (${userName.length}) < Constants.minUserNameLen (${Constants.minUserNameLen})")
        return CreateRoomResponsePayload.fail(Status.BadParam)
      }

      if (userName.length > Constants.maxUserNameLen) {
        println("userName.length (${userName.length}) > Constants.minUserNameLen (${Constants.minUserNameLen})")
        return CreateRoomResponsePayload.fail(Status.BadParam)
      }
    }

    return null
  }
}