package handler

import core.Status
import core.byte_sink.ByteSink
import core.exception.PacketDeserializationException
import core.exception.UnknownPacketVersionException
import core.model.drainable.chat_message.ChatMessageType
import core.packet.SendChatMessagePacket
import core.response.NewChatMessageResponsePayload
import core.response.SendChatMessageResponsePayload
import manager.ChatRoomManager
import manager.ConnectionManager

class SendChatMessageHandler(
  private val connectionManager: ConnectionManager,
  private val chatRoomManager: ChatRoomManager
) : BasePacketHandler() {

  override suspend fun handle(byteSink: ByteSink, clientId: String) {
    val packet = try {
      SendChatMessagePacket.fromByteSink(byteSink)
    } catch (error: PacketDeserializationException) {
      error.printStackTrace()
      connectionManager.sendResponse(clientId, SendChatMessageResponsePayload.fail(Status.BadPacket))
      return
    }

    val packetVersion = SendChatMessagePacket.PacketVersion.fromShort(packet.getPacketVersion())
    when (packetVersion) {
      SendChatMessagePacket.PacketVersion.V1 -> handleInternalV1(packet, clientId)
      SendChatMessagePacket.PacketVersion.Unknown -> throw UnknownPacketVersionException(packetVersion.value)
    }
  }

  private suspend fun handleInternalV1(packet: SendChatMessagePacket, clientId: String) {
    val clientMessageId = packet.clientMessageId
    val roomName = packet.roomName
    val userName = packet.userName
    val message = packet.message

    //TODO: validate
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

      connectionManager.sendResponse(clientId, SendChatMessageResponsePayload.fail(Status.BadParam))
      return
    }

    println("User ($userName) is trying to send a message ($message)")

    val chatRoom = chatRoomManager.getChatRoom(roomName)
    if (chatRoom == null) {
      println("Room with name (${roomName}) does not exist")
      connectionManager.sendResponse(clientId, SendChatMessageResponsePayload.fail(Status.ChatRoomDoesNotExist))
      return
    }

    val user = chatRoomManager.getUser(clientId, roomName, userName)
    if (user == null) {
      println("User ($userName) does not exist in the room ($roomName)")
      connectionManager.sendResponse(clientId, SendChatMessageResponsePayload.fail(Status.UserDoesNotExistInTheRoom))
      return
    }

    val serverMessageId = chatRoom.addMessage(clientId, ChatMessageType.Text, clientMessageId, userName, message)
    val response = NewChatMessageResponsePayload.success(serverMessageId, clientMessageId, roomName, userName, message)

    for (user in chatRoom.getEveryoneExcept(userName)) {
      connectionManager.sendResponse(user.clientId, response)
    }

    connectionManager.sendResponse(clientId, SendChatMessageResponsePayload.success(roomName, serverMessageId, clientMessageId))
    println("Message ($message) has been successfully sent in room (${roomName}) by user (${userName})")
  }
}