package manager

import core.ChatRoom
import core.PublicChatRoom
import core.User
import core.UserInRoom
import core.security.SecurityUtils
import core.utils.TimeUtils
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock

class ChatRoomManager {
  private val mutex = Mutex()
  private val defaultChatRoomLength = 10
  private val maxUsersInRoom = 100

  private val chatRooms = mutableMapOf<String, ChatRoom>()

  suspend fun createChatRoom(
    isPublic: Boolean = true,
    chatRoomName: String? = null,
    chatRoomPasswordHash: String? = null
  ): ChatRoom {
    val roomName = (chatRoomName ?: SecurityUtils.Generation.generateRandomString(defaultChatRoomLength))
    val chatRoom = ChatRoom(roomName, chatRoomPasswordHash, isPublic, TimeUtils.getCurrentTime())

    mutex.withLock {
      require(chatRooms.size < maxUsersInRoom)
      require(!chatRooms.containsKey(roomName))

      chatRooms[roomName] = chatRoom
    }

    return chatRoom
  }

  suspend fun joinRoom(
    chatRoomName: String,
    user: User
  ): Boolean {
    return mutex.withLock {
      if (chatRooms.containsKey(chatRoomName)) {
        return@withLock false
      }

      val chatRoom = chatRooms[chatRoomName]
      if (chatRoom == null) {
        return@withLock false
      }

      if (chatRoom.containsUser(user.userName)) {
        return@withLock false
      }

      chatRoom.addUser(UserInRoom(user))

      //TODO: broadcast to everyone that user has joined the room
      return@withLock true
    }
  }

  suspend fun leaveRoom(
    chatRoomName: String,
    user: User
  ): Boolean {
    return mutex.withLock {
      if (chatRooms.containsKey(chatRoomName)) {
        return@withLock false
      }

      val chatRoom = chatRooms[chatRoomName]
      if (chatRoom == null) {
        return@withLock false
      }

      if (!chatRoom.containsUser(user.userName)) {
        return@withLock false
      }

      chatRoom.removeUser(user.userName)

      //TODO: broadcast to everyone that user has left the room
      return@withLock true
    }
  }

  suspend fun exists(chatRoomName: String? = null): Boolean {
    if (chatRoomName == null) {
      return false
    }

    return mutex.withLock { chatRooms.containsKey(chatRoomName) }
  }

  suspend fun getAllPublicRooms(): List<PublicChatRoom> {
    return mutex.withLock {
      return@withLock chatRooms.values
        .filter { chatRoom -> chatRoom.isPublic }
        .map { chatRoom ->
          val copy = chatRoom.copy()
          return@map PublicChatRoom(copy.roomName, copy.countUsers().toShort())
        }
    }
  }
}