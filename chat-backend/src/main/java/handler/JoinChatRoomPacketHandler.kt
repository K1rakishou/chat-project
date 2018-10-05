package handler

import core.Status
import core.User
import core.byte_sink.ByteSink
import core.exception.UnknownPacketVersion
import core.extensions.isNullOrEmpty
import core.model.drainable.PublicUserInChat
import core.packet.JoinChatRoomPacket
import core.response.JoinChatRoomResponsePayload
import core.response.UserHasJoinedResponsePayload
import manager.ChatRoomManager
import manager.ConnectionManager

class JoinChatRoomPacketHandler(
  private val connectionManager: ConnectionManager,
  private val chatRoomManager: ChatRoomManager
) : BasePacketHandler() {

  override suspend fun handle(packetId: Long, byteSink: ByteSink, clientAddress: String) {
    val packetVersion = JoinChatRoomPacket.PacketVersion.fromShort(byteSink.readShort())

    when (packetVersion) {
      JoinChatRoomPacket.PacketVersion.V1 -> handleInternalV1(packetId, byteSink, clientAddress)
      JoinChatRoomPacket.PacketVersion.Unknown -> throw UnknownPacketVersion()
    }
  }

  private suspend fun handleInternalV1(packetId: Long, byteSink: ByteSink, clientAddress: String) {
    val ecPublicKey = byteSink.readByteArray()
    val userName = byteSink.readString()
    val roomName = byteSink.readString()
    val roomPasswordHash = byteSink.readString()

    if ((ecPublicKey == null || ecPublicKey.isEmpty()) ||
        (userName == null || userName.isEmpty()) ||
        (roomName == null || roomName.isEmpty())) {
      if (ecPublicKey.isNullOrEmpty()) {
        println("ecPublicKey is NULL or empty (${ecPublicKey})")
      }

      if (userName.isNullOrEmpty()) {
        println("userName is NULL or empty (${userName})")
      }

      if (roomName.isNullOrEmpty()) {
        println("roomName is NULL or empty (${roomName})")
      }

      connectionManager.sendResponse(clientAddress, JoinChatRoomResponsePayload.fail(Status.BadParam))
      return
    }

    if (!chatRoomManager.exists(roomName)) {
      println("Room with name (${roomName}) does not exist")
      connectionManager.sendResponse(clientAddress, JoinChatRoomResponsePayload.fail(Status.ChatRoomDoesNotExist))
      return
    }

    if (chatRoomManager.hasPassword(roomName)) {
      if (roomPasswordHash == null || roomPasswordHash.isEmpty()) {
        println("Room with name ${roomName} is password protected and user has not provided password (${roomPasswordHash})")
        connectionManager.sendResponse(clientAddress, JoinChatRoomResponsePayload.fail(Status.BadParam))
        return
      }

      if (!chatRoomManager.passwordsMatch(roomName, roomPasswordHash)) {
        println("Provided by the user password does not match room's password (${roomPasswordHash})")
        connectionManager.sendResponse(clientAddress, JoinChatRoomResponsePayload.fail(Status.WrongRoomPassword))
        return
      }
    }

    val newUser = User(userName, clientAddress, ecPublicKey)
    val chatRoom = chatRoomManager.joinRoom(roomName, newUser)

    if (chatRoom == null) {
      println("Could not join the room (${roomName}) by user (${newUser.clientAddress}, ${newUser.userName})")
      connectionManager.sendResponse(clientAddress, JoinChatRoomResponsePayload.fail(Status.CouldNotJoinChatRoom))
      return
    }

    val usersInRoom = chatRoom.getEveryoneExcept(userName)
    val publicUserInChatList = mutableListOf<PublicUserInChat>()

    println("There are ${usersInRoom.size} users in room")

    //get all info from all users in the chat room
    for (userInRoom in usersInRoom) {
      val publicUserInChat = PublicUserInChat(userInRoom.user.userName, userInRoom.user.ecPublicKey)

      //send to every user in the chat room that a new user has joined
      val newPublicUser = PublicUserInChat(newUser.userName, newUser.ecPublicKey)
      connectionManager.sendResponse(userInRoom.user.clientAddress, UserHasJoinedResponsePayload.success(newPublicUser))

      publicUserInChatList += publicUserInChat
    }

    //send back list of users in the chat room
    connectionManager.sendResponse(clientAddress, JoinChatRoomResponsePayload.success(publicUserInChatList))
  }
}