package handler

import core.Status
import core.byte_sink.ByteSink
import core.exception.UnknownPacketVersion
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
    val packetVersion = SendChatMessagePacket.PacketVersion.fromShort(byteSink.readShort())

    when (packetVersion) {
      SendChatMessagePacket.PacketVersion.V1 -> handleInternalV1(packetId, byteSink, clientAddress)
      SendChatMessagePacket.PacketVersion.Unknown -> throw UnknownPacketVersion()
    }
  }

  private suspend fun handleInternalV1(packetId: Long, byteSink: ByteSink, clientAddress: String) {
    val packet = SendChatMessagePacket.fromByteSink(byteSink)
    val messageId = packet.messageId
    val roomName = packet.roomName
    val userName = packet.userName
    val message = packet.message

    if (roomName.isEmpty() || userName.isEmpty() || message.isEmpty()) {
      connectionManager.sendResponse(clientAddress, SendChatMessageResponsePayload.fail(Status.BadParam))
      return
    }

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

    for (userInRoom in roomParticipants) {
      val response = NewChatMessageResponsePayload.success(messageId, roomName, userName, message)
      connectionManager.sendResponse(userInRoom.user.clientAddress, response)
    }

    connectionManager.sendResponse(clientAddress, SendChatMessageResponsePayload.success())
  }
}