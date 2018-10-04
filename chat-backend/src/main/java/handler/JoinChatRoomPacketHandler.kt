package handler

import core.PublicUserInChat
import core.Status
import core.User
import core.byte_sink.ByteSink
import core.exception.UnknownPacketVersion
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
    val packetVersion = JoinChatRoomPacketPayload.PacketVersion.fromShort(byteSink.readShort())

    when (packetVersion) {
      JoinChatRoomPacketPayload.PacketVersion.V1 -> handleInternalV1(packetId, byteSink, clientAddress)
      JoinChatRoomPacketPayload.PacketVersion.Unknown -> throw UnknownPacketVersion()
    }
  }

  private suspend fun handleInternalV1(packetId: Long, byteSink: ByteSink, clientAddress: String) {
    val ecPublicKey = byteSink.readByteArray()
    val userName = byteSink.readString()!!
    val roomName = byteSink.readString()!!
    val roomPasswordHash = byteSink.readString()

    if (!chatRoomManager.exists(roomName)) {
      connectionManager.send(clientAddress, JoinChatRoomResponsePayload.fail(Status.ChatRoomDoesNotExist))
      return
    }

    val newUser = User(userName, clientAddress, ecPublicKey)


    //TODO: handle rooms with password as well
    if (!chatRoomManager.joinRoom(roomName, newUser)) {
      connectionManager.send(clientAddress, JoinChatRoomResponsePayload.fail(Status.CouldNotJoinChatRoom))
      return
    }

    val chatRoom = chatRoomManager.getChatRoom(roomName)
    if (chatRoom == null) {
      connectionManager.send(clientAddress, JoinChatRoomResponsePayload.fail(Status.ChatRoomDoesNotExist))
      return
    }

    val usersInRoom = chatRoom.getEveryoneExcept(userName)
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
  }
}