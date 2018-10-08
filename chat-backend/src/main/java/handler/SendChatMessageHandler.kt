package handler

import core.Status
import core.byte_sink.ByteSink
import core.exception.PacketDeserializationException
import core.exception.UnknownPacketVersionException
import core.model.drainable.chat_message.TextChatMessage
import core.packet.SendChatMessagePacket
import core.response.NewChatMessageResponsePayload
import core.response.SendChatMessageResponsePayload
import manager.ChatRoomManager
import manager.ConnectionManager

class SendChatMessageHandler(
  private val connectionManager: ConnectionManager,
  private val chatRoomManager: ChatRoomManager
) : BasePacketHandler() {

  override suspend fun handle(packetId: Long, byteSink: ByteSink, clientAddress: String) {
    val packet = try {
      SendChatMessagePacket.fromByteSink(byteSink)
    } catch (error: PacketDeserializationException) {
      error.printStackTrace()
      connectionManager.sendResponse(clientAddress, SendChatMessageResponsePayload.fail(Status.BadPacket))
      return
    }

    val packetVersion = SendChatMessagePacket.PacketVersion.fromShort(packet.packetVersion)

    when (packetVersion) {
      SendChatMessagePacket.PacketVersion.V1 -> handleInternalV1(packet, packetId, clientAddress)
      SendChatMessagePacket.PacketVersion.Unknown -> throw UnknownPacketVersionException(packetVersion.value)
    }
  }

  private suspend fun handleInternalV1(packet: SendChatMessagePacket, packetId: Long, clientAddress: String) {
    val messageId = packet.messageId
    val roomName = packet.roomName
    val userName = packet.userName
    val message = packet.message

    if (roomName.isEmpty() || userName.isEmpty() || message.isEmpty()) {
      if (roomName.isEmpty()) {
        println("roomName is empty")
      }

      if (userName.isEmpty()) {
        println("userName is empty")
      }

      if (message.isEmpty()) {
        println("message is empty")
      }

      connectionManager.sendResponse(clientAddress, SendChatMessageResponsePayload.fail(Status.BadParam))
      return
    }

    println("User ($userName) is trying to send a message ($message)")

    val chatRoom = chatRoomManager.getChatRoom(roomName)
    if (chatRoom == null) {
      println("Room with name (${roomName}) does not exist")
      connectionManager.sendResponse(clientAddress, SendChatMessageResponsePayload.fail(Status.ChatRoomDoesNotExist))
      return
    }

    val user = chatRoomManager.getUser(roomName, userName)
    if (user == null) {
      println("User ($userName) does not exist in the room $roomName")
      connectionManager.sendResponse(clientAddress, SendChatMessageResponsePayload.fail(Status.UserDoesNotExistInTheRoom))
      return
    }

    chatRoom.addMessage(TextChatMessage(messageId, userName, message))
    val roomParticipants = chatRoom.getEveryoneExcept(userName)
    val response = NewChatMessageResponsePayload.success(messageId, roomName, userName, message)

    for (userInRoom in roomParticipants) {
      connectionManager.sendResponse(userInRoom.user.clientAddress, response)
    }

    connectionManager.sendResponse(clientAddress, SendChatMessageResponsePayload.success())
    println("Message ($message) has been successfully sent in room ${roomName} by user ${userName}")
  }
}