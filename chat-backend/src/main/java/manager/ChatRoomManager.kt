package manager

import core.ChatRoom
import core.model.drainable.PublicChatRoom
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
  ): ChatRoom? {
    return mutex.withLock {
      if (!chatRooms.containsKey(chatRoomName)) {
        return@withLock null
      }

      val chatRoom = chatRooms[chatRoomName]!!
      if (chatRoom.containsUser(user.userName)) {
        return@withLock null
      }

      chatRoom.addUser(UserInRoom(user))
      return@withLock chatRoom
    }
  }

  suspend fun leaveRoom(
    chatRoomName: String,
    user: User
  ): Boolean {
    return mutex.withLock {
      if (!chatRooms.containsKey(chatRoomName)) {
        return@withLock false
      }

      val chatRoom = chatRooms[chatRoomName]!!
      if (!chatRoom.containsUser(user.userName)) {
        return@withLock false
      }

      chatRoom.removeUser(user.userName)

      //TODO: broadcast to everyone that user has left the room
      return@withLock true
    }
  }

  suspend fun alreadyJoined(chatRoomName: String, userName: String): Boolean {
    return mutex.withLock {
      require(chatRooms.containsKey(chatRoomName))

      return@withLock chatRooms[chatRoomName]!!.containsUser(userName)
    }
  }

  suspend fun exists(chatRoomName: String? = null): Boolean {
    requireNotNull(chatRoomName)

    return mutex.withLock { chatRooms.containsKey(chatRoomName) }
  }

  suspend fun hasPassword(chatRoomName: String): Boolean {
    return mutex.withLock {
      require(chatRooms.containsKey(chatRoomName))

      return@withLock chatRooms[chatRoomName]!!.hasPassword()
    }
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

  suspend fun passwordsMatch(chatRoomName: String, chatRoomPassword: String): Boolean {
    return mutex.withLock {
      require(chatRooms.containsKey(chatRoomName))

      return@withLock chatRooms[chatRoomName]!!.passwordsMatch(chatRoomPassword)
    }
  }

  suspend fun getChatRoom(roomName: String): ChatRoom? {
    return mutex.withLock {
      return@withLock chatRooms[roomName]?.copy()
    }
  }
}