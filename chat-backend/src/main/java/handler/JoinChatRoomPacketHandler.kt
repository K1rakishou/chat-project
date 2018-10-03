package handler

import core.PublicUserInChat
import core.Status
import core.User
import core.byte_sink.ByteSink
import core.packet.JoinChatRoomPacketPayload
import core.response.JoinChatRoomResponsePayload
import core.response.UserHasJoinedResponsePayload
import manager.ChatRoomManager
import manager.ConnectionManager

class JoinChatRoomPacketHandler(
  private val connectionManager: ConnectionManager,
  private val chatRoomManager: ChatRoomManager
) : BasePacketHandler() {

  override suspend fun handle(packetId: Long, byteSink: ByteSink, clientAddress: String) {
    val packet = JoinChatRoomPacketPayload.fromByteSink(byteSink)
    val packetVersion = JoinChatRoomPacketPayload.PacketVersion.fromShort(packet.packetVersion)

    when (packetVersion) {
      JoinChatRoomPacketPayload.PacketVersion.V1 -> handleInternalV1(packetId, packet, clientAddress)
    }
  }

  private suspend fun handleInternalV1(packetId: Long, packet: JoinChatRoomPacketPayload, clientAddress: String) {
    try {
      if (!chatRoomManager.exists(packet.roomName)) {
        connectionManager.send(clientAddress, JoinChatRoomResponsePayload.fail(Status.ChatRoomDoesNotExist))
        return
      }

      val newUser = User(packet.userName, clientAddress, packet.ecPublicKey)

      if (!chatRoomManager.joinRoom(packet.roomName, newUser)) {
        connectionManager.send(clientAddress, JoinChatRoomResponsePayload.fail(Status.CouldNotJoinChatRoom))
        return
      }

      val chatRoom = chatRoomManager.getChatRoom(packet.roomName)
      if (chatRoom == null) {
        connectionManager.send(clientAddress, JoinChatRoomResponsePayload.fail(Status.ChatRoomDoesNotExist))
        return
      }

      val usersInRoom = chatRoom.getEveryoneExcept(packet.userName)
      val publicUserInChatList = mutableListOf<PublicUserInChat>()

      //get all info from all users in the chat room
      for (userInRoom in usersInRoom) {
        val publicUserInChat = PublicUserInChat(userInRoom.user.userName, userInRoom.user.ecPublicKey)

        //send to every user in the chat room that a new use has joined
        val newPublicUser = PublicUserInChat(newUser.userName, newUser.ecPublicKey)
        connectionManager.send(userInRoom.user.clientAddress, UserHasJoinedResponsePayload.success(newPublicUser))

        publicUserInChatList += publicUserInChat
      }

      //send back list of users in the chat room
      connectionManager.send(clientAddress, JoinChatRoomResponsePayload.success(publicUserInChatList))
    } catch (error: Throwable) {
      connectionManager.send(clientAddress, JoinChatRoomResponsePayload.fail(Status.UnknownError))
    }
  }
}