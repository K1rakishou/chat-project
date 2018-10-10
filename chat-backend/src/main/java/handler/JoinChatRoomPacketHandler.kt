package handler

import core.Status
import core.User
import core.byte_sink.ByteSink
import core.exception.PacketDeserializationException
import core.exception.UnknownPacketVersionException
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

  override suspend fun handle(byteSink: ByteSink, clientAddress: String) {
    val packet = try {
      JoinChatRoomPacket.fromByteSink(byteSink)
    } catch (error: PacketDeserializationException) {
      error.printStackTrace()
      connectionManager.sendResponse(clientAddress, JoinChatRoomResponsePayload.fail(Status.BadPacket))
      return
    }

    val packetVersion = JoinChatRoomPacket.PacketVersion.fromShort(packet.packetVersion)

    when (packetVersion) {
      JoinChatRoomPacket.PacketVersion.V1 -> handleInternalV1(packet, clientAddress)
      JoinChatRoomPacket.PacketVersion.Unknown -> throw UnknownPacketVersionException(packetVersion.value)
    }
  }

  private suspend fun handleInternalV1(packet: JoinChatRoomPacket, clientAddress: String) {
    val ecPublicKey = packet.ecPublicKey
    val userName = packet.userName
    val roomName = packet.roomName
    val roomPasswordHash = packet.roomPasswordHash

    if (ecPublicKey.isEmpty() || userName.isEmpty() || roomName.isEmpty()) {
      if (ecPublicKey.isNullOrEmpty()) {
        println("ecPublicKey is empty (${ecPublicKey})")
      }

      if (userName.isEmpty()) {
        println("userName is empty (${userName})")
      }

      if (roomName.isEmpty()) {
        println("chatRoomName is empty (${roomName})")
      }

      connectionManager.sendResponse(clientAddress, JoinChatRoomResponsePayload.fail(Status.BadParam))
      return
    }

    println("User (${userName}) trying to join room (${roomName})")

    if (!chatRoomManager.exists(roomName)) {
      println("Room with name (${roomName}) does not exist")
      connectionManager.sendResponse(clientAddress, JoinChatRoomResponsePayload.fail(Status.ChatRoomDoesNotExist))
      return
    }

    if (chatRoomManager.alreadyJoined(roomName, userName)) {
      //we have already joined this room, no need to add the user in the room second time and notify everyone in the room about it
      println("User (${userName}) has already joined room (${roomName})")

      val chatRoom = chatRoomManager.getChatRoom(roomName)
      if (chatRoom == null) {
        println("Room with name (${roomName}) does not exist")
        connectionManager.sendResponse(clientAddress, JoinChatRoomResponsePayload.fail(Status.ChatRoomDoesNotExist))
        return
      }

      //just collect users's info and send it back
      val usersInRoom = chatRoom.getEveryoneExcept(userName)
      val publicUserInChatList = usersInRoom
        .map { userInRoom -> PublicUserInChat(userInRoom.user.userName, userInRoom.user.ecPublicKey) }

      val messageHistory = chatRoom.getMessageHistory()
      val response = JoinChatRoomResponsePayload.success(chatRoom.chatRoomName, messageHistory, publicUserInChatList)

      connectionManager.sendResponse(clientAddress, response)
      return
    }

    if (chatRoomManager.hasPassword(roomName)) {
      if (roomPasswordHash == null || roomPasswordHash.isEmpty()) {
        println("Room with name (${roomName}) is password protected and user has not provided password (${roomPasswordHash})")
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

    val roomParticipants = chatRoom.getEveryoneExcept(userName)
    val publicUserInChatList = mutableListOf<PublicUserInChat>()

    println("There are ${roomParticipants.size} users in room")

    //get all info from all users in the chat room
    for (userInRoom in roomParticipants) {
      val publicUserInChat = PublicUserInChat(userInRoom.user.userName, userInRoom.user.ecPublicKey)

      //send to every user in the chat room that a new user has joined
      val newPublicUser = PublicUserInChat(newUser.userName, newUser.ecPublicKey)
      val response = UserHasJoinedResponsePayload.success(chatRoom.chatRoomName, newPublicUser)
      connectionManager.sendResponse(userInRoom.user.clientAddress, response)

      publicUserInChatList += publicUserInChat
    }

    //send back list of users in the chat room
    val messageHistory = chatRoom.getMessageHistory()
    val response = JoinChatRoomResponsePayload.success(chatRoom.chatRoomName, messageHistory, publicUserInChatList)
    connectionManager.sendResponse(clientAddress, response)

    println("User (${userName}) has successfully joined room (${roomName})")
  }
}