package handler

import core.Constants
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

    if (packet.chatRoomName.isNullOrEmpty() || packet.chatRoomImageUrl.isNullOrEmpty()) {
      if (packet.chatRoomName.isNullOrEmpty()) {
        println("chatRoomName is null or empty: ${packet.chatRoomName}")
      }

      if (packet.chatRoomImageUrl.isNullOrEmpty()) {
        println("chatRoomImageUrl is null or empty: ${packet.chatRoomImageUrl}")
      }

      return CreateRoomResponsePayload.fail(Status.BadParam)
    }

    val chatRoomName = packet.chatRoomName!!
    val chatRoomImageUrl = packet.chatRoomImageUrl!!

    if (chatRoomName.length < Constants.minChatRoomNameLen || chatRoomName.length > Constants.maxChatRoomNameLength) {
      if (chatRoomName.length < Constants.minChatRoomNameLen) {
        println("chatRoomName.length (${chatRoomName.length}) < Constants.minChatRoomNameLen (${Constants.minChatRoomNameLen})")
      }

      if (chatRoomName.length > Constants.maxChatRoomNameLength) {
        println("chatRoomName.length (${chatRoomName.length}) > Constants.maxChatRoomNameLength (${Constants.maxChatRoomNameLength})")
      }

      return CreateRoomResponsePayload.fail(Status.BadParam)
    }

    if (!isImageUrlValid(chatRoomImageUrl)) {
      println("chatRoomImageUrl is not valid")
      return CreateRoomResponsePayload.fail(Status.BadParam)
    }

    val chatRoom = chatRoomManager.createChatRoom(
      isPublic = packet.isPublic,
      chatRoomName = packet.chatRoomName!!,
      chatRoomPasswordHash = packet.chatRoomPasswordHash,
      chatRoomImageUrl = packet.chatRoomImageUrl!!
    )

    println("ChatRoom ${chatRoom} has been successfully created!")
    return CreateRoomResponsePayload.success()
  }

  private fun isImageUrlValid(url: String): Boolean {
    //for now only allow images from imgur.com
    //https://i.imgur.com/xxx.jpg

    val split1 = url.split("//")
    if (split1[0] != "https:") {
      return false
    }

    val split2 = split1[1].split("/")
    if (!split2[0].startsWith("i.imgur.com")) {
      return false
    }

    val split3 = split2[1].split('.')
    return split3[1] == "jpg" || split3[1] == "png" || split3[1] == "jpeg"
  }

}