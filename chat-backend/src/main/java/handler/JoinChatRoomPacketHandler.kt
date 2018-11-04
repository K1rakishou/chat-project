package handler

import core.Status
import core.User
import core.byte_sink.ByteSink
import core.exception.PacketDeserializationException
import core.exception.UnknownPacketVersionException
import core.model.drainable.PublicUserInChat
import core.packet.JoinChatRoomPacket
import core.response.JoinChatRoomResponsePayload
import core.response.UserHasJoinedResponsePayload
import core.utils.Validators
import manager.ChatRoomManager
import manager.ConnectionManager

class JoinChatRoomPacketHandler(
  private val connectionManager: ConnectionManager,
  private val chatRoomManager: ChatRoomManager
) : BasePacketHandler() {

  override suspend fun handle(byteSink: ByteSink, clientId: String) {
    val packet = try {
      JoinChatRoomPacket.fromByteSink(byteSink)
    } catch (error: PacketDeserializationException) {
      error.printStackTrace()
      connectionManager.sendResponse(clientId, JoinChatRoomResponsePayload.fail(Status.BadPacket))
      return
    }

    val packetVersion = JoinChatRoomPacket.PacketVersion.fromShort(packet.getPacketVersion())
    when (packetVersion) {
      JoinChatRoomPacket.PacketVersion.V1 -> handleInternalV1(packet, clientId)
      JoinChatRoomPacket.PacketVersion.Unknown -> throw UnknownPacketVersionException(packetVersion.value)
    }
  }

  private suspend fun handleInternalV1(packet: JoinChatRoomPacket, clientId: String) {
    val userName = packet.userName
    val roomName = packet.roomName
    val roomPasswordHash = packet.roomPasswordHash

    val status = validateV1(userName, roomName, roomPasswordHash)
    if (status != null) {
      connectionManager.sendResponse(clientId, JoinChatRoomResponsePayload.fail(status))
      return
    }

    println("User (${userName}) trying to join room (${roomName})")

    if (!chatRoomManager.exists(roomName)) {
      println("Room with name (${roomName}) does not exist")
      connectionManager.sendResponse(clientId, JoinChatRoomResponsePayload.fail(Status.ChatRoomDoesNotExist))
      return
    }

    if (chatRoomManager.alreadyJoined(clientId, roomName, userName)) {
      //we have already joined this room, no need to add the user in the room second time and notify everyone in the room about it
      println("User (${userName}) has already joined room (${roomName})")

      val chatRoom = chatRoomManager.getChatRoom(roomName)
      if (chatRoom == null) {
        println("Room with name (${roomName}) does not exist")
        connectionManager.sendResponse(clientId, JoinChatRoomResponsePayload.fail(Status.ChatRoomDoesNotExist))
        return
      }

      //just collect users's info and send it back
      val usersInRoom = chatRoom.getEveryoneExcept(userName)
      val publicUserInChatList = usersInRoom
        .map { userInRoom -> PublicUserInChat(userInRoom.user.userName) }

      val messageHistory = chatRoom.getMessageHistory(clientId)
      val response = JoinChatRoomResponsePayload.success(chatRoom.chatRoomName, userName, messageHistory, publicUserInChatList)

      connectionManager.sendResponse(clientId, response)
      return
    }

    if (chatRoomManager.roomContainsNickname(roomName, userName)) {
      println("Room with name (${roomName}) already contains user with userName: ${userName}")
      connectionManager.sendResponse(clientId, JoinChatRoomResponsePayload.fail(Status.UserNameAlreadyTaken))
      return
    }

    if (chatRoomManager.hasPassword(roomName)) {
      if (roomPasswordHash == null || roomPasswordHash.isEmpty()) {
        println("Room with name (${roomName}) is password protected and user has not provided password (${roomPasswordHash})")
        connectionManager.sendResponse(clientId, JoinChatRoomResponsePayload.fail(Status.BadParam))
        return
      }

      if (!chatRoomManager.passwordsMatch(roomName, roomPasswordHash)) {
        println("Password provided by the user does not match room's password")
        connectionManager.sendResponse(clientId, JoinChatRoomResponsePayload.fail(Status.WrongRoomPassword))
        return
      }
    }

    val newUser = User(userName, clientId)
    val chatRoom = chatRoomManager.joinRoom(clientId, roomName, newUser)

    if (chatRoom == null) {
      println("Could not join the room (${roomName}) by user (${newUser.clientId}, ${newUser.userName})")
      connectionManager.sendResponse(clientId, JoinChatRoomResponsePayload.fail(Status.CouldNotJoinChatRoom))
      return
    }

    val roomParticipants = chatRoom.getEveryoneExcept(userName)
    val publicUserInChatList = mutableListOf<PublicUserInChat>()

    println("There are ${roomParticipants.size} users in room")

    //get all info from all users in the chat room
    for (userInRoom in roomParticipants) {
      val publicUserInChat = PublicUserInChat(userInRoom.user.userName)

      //send to every user in the chat room that a new user has joined
      val newPublicUser = PublicUserInChat(newUser.userName)
      val response = UserHasJoinedResponsePayload.success(chatRoom.chatRoomName, newPublicUser)
      connectionManager.sendResponse(userInRoom.user.clientId, response)

      publicUserInChatList += publicUserInChat
    }

    //send back list of users in the chat room
    val messageHistory = chatRoom.getMessageHistory(clientId)
    val response = JoinChatRoomResponsePayload.success(chatRoom.chatRoomName, userName, messageHistory, publicUserInChatList)
    connectionManager.sendResponse(clientId, response)

    println("User (${userName}) has successfully joined room (${roomName})")
  }

  private fun validateV1(userName: String, roomName: String, roomPasswordHash: String?): Status? {
    var status: Status? = null

    status = Validators.validateUserName(userName)
    if (status != null) {
      return status
    }

    status = Validators.validateChatRoomName(roomName)
    if (status != null) {
      return status
    }

    status = Validators.validateChatRoomPasswordHash(roomPasswordHash)
    if (status != null) {
      return status
    }

    return null
  }
}