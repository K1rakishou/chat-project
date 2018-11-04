package manager

import core.ChatRoom
import core.Constants
import core.User
import core.UserInRoom
import core.extensions.myWithLock
import core.model.drainable.PublicChatRoom
import core.utils.TimeUtils
import kotlinx.coroutines.sync.Mutex

class ChatRoomManager {
  private val mutex = Mutex()
  private val clientIdToRoomNameCache = mutableMapOf<String, MutableList<RoomNameUserNamePair>>()
  private val chatRooms = mutableMapOf<String, ChatRoom>()

  @Deprecated(message = "For tests only!")
  fun __getClientIdToRoomNameCache() = clientIdToRoomNameCache
  @Deprecated(message = "For tests only!")
  fun __getChatRooms() = chatRooms

  suspend fun createChatRoom(
    isPublic: Boolean = true,
    chatRoomName: String,
    chatRoomImageUrl: String,
    chatRoomPasswordHash: String? = null
  ): ChatRoom {
    val chatRoom = ChatRoom(chatRoomName, chatRoomImageUrl, chatRoomPasswordHash, isPublic, TimeUtils.getCurrentTime())

    mutex.myWithLock {
      require(chatRooms.size < Constants.maxUsersInRoomCount)
      require(!chatRooms.containsKey(chatRoomName))

      chatRooms[chatRoomName] = chatRoom
    }

    return chatRoom
  }

  suspend fun joinRoom(
    clientId: String,
    chatRoomName: String,
    user: User
  ): ChatRoom? {
    return mutex.myWithLock {
      if (!chatRooms.containsKey(chatRoomName)) {
        return@myWithLock null
      }

      val chatRoom = chatRooms[chatRoomName]!!
      if (chatRoom.containsUser(user.userName)) {
        return@myWithLock null
      }

      if (!chatRoom.addUser(UserInRoom(user))) {
        return@myWithLock null
      }

      clientIdToRoomNameCache.putIfAbsent(clientId, mutableListOf())
      clientIdToRoomNameCache[clientId]!!.add(RoomNameUserNamePair(chatRoomName, user.userName))

      return@myWithLock chatRoom
    }
  }

  suspend fun leaveRoom(
    clientId: String,
    chatRoomName: String,
    user: User
  ): Boolean {
    return mutex.myWithLock {
      if (!chatRooms.containsKey(chatRoomName)) {
        return@myWithLock false
      }

      val chatRoom = chatRooms[chatRoomName]!!
      if (!chatRoom.containsUser(user.userName)) {
        return@myWithLock false
      }

      clientIdToRoomNameCache[clientId]!!.remove(RoomNameUserNamePair(chatRoomName, user.userName))
      if (clientIdToRoomNameCache[clientId]!!.isEmpty()) {
        clientIdToRoomNameCache.remove(clientId)
      }

      chatRoom.removeUser(user.userName)
      return@myWithLock true
    }
  }

  suspend fun leaveAllRooms(clientId: String): List<RoomNameUserNamePair> {
    return mutex.myWithLock {
      val list = mutableListOf<RoomNameUserNamePair>()

      if (clientIdToRoomNameCache[clientId] == null) {
        return@myWithLock list
      }

      if (clientIdToRoomNameCache[clientId]!!.isEmpty()) {
        clientIdToRoomNameCache.remove(clientId)
        return@myWithLock list
      }

      for ((roomName, userName) in clientIdToRoomNameCache[clientId]!!) {
        chatRooms[roomName]?.removeUser(userName)
        list += RoomNameUserNamePair(roomName, userName)
      }

      clientIdToRoomNameCache.remove(clientId)
      return@myWithLock list
    }
  }

  suspend fun alreadyJoined(clientId: String, chatRoomName: String, userName: String): Boolean {
    return mutex.myWithLock {
      val cacheEntry = clientIdToRoomNameCache[clientId]
      if (cacheEntry == null) {
        return@myWithLock false
      }

      return@myWithLock cacheEntry.any { it.roomName == chatRoomName && it.userName == userName }
    }
  }

  suspend fun roomContainsNickname(chatRoomName: String, userName: String): Boolean {
    return mutex.myWithLock {
      require(chatRooms.containsKey(chatRoomName))
      return@myWithLock chatRooms[chatRoomName]!!.containsUser(userName)
    }
  }

  suspend fun exists(chatRoomName: String? = null): Boolean {
    requireNotNull(chatRoomName)
    return mutex.myWithLock { chatRooms.containsKey(chatRoomName) }
  }

  suspend fun hasPassword(chatRoomName: String): Boolean {
    return mutex.myWithLock {
      require(chatRooms.containsKey(chatRoomName))
      return@myWithLock chatRooms[chatRoomName]!!.hasPassword()
    }
  }

  suspend fun getAllPublicRooms(): List<PublicChatRoom> {
    return mutex.myWithLock {
      return@myWithLock chatRooms.values
        .filter { chatRoom -> chatRoom.isPublic }
        .map { chatRoom ->
          val copy = chatRoom.copy()
          return@map PublicChatRoom(copy.chatRoomName, copy.chatRoomImageUrl)
        }
    }
  }

  suspend fun passwordsMatch(chatRoomName: String, chatRoomPassword: String): Boolean {
    return mutex.myWithLock {
      require(chatRooms.containsKey(chatRoomName))

      return@myWithLock chatRooms[chatRoomName]!!.passwordsMatch(chatRoomPassword)
    }
  }

  suspend fun getChatRoom(roomName: String): ChatRoom? {
    return mutex.myWithLock {
      return@myWithLock chatRooms[roomName]
    }
  }

  suspend fun getUser(clientId: String, roomName: String, userName: String): UserInRoom? {
    return mutex.myWithLock {
      val joinedRoom = clientIdToRoomNameCache[clientId]
        ?.any { it.roomName == roomName && it.userName == userName }
        ?: false

      if (!joinedRoom) {
        return@myWithLock null
      }

      return@myWithLock chatRooms[roomName]?.getUser(userName)
    }
  }

  data class RoomNameUserNamePair(
    val roomName: String,
    val userName: String
  )
}