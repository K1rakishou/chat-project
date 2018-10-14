package manager

import core.ChatRoom
import core.Constants
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
  private val clientAddressToRoomNameCache = mutableMapOf<String, MutableList<RoomNameUserNamePair>>()
  private val chatRooms = mutableMapOf<String, ChatRoom>()

  @Deprecated(message = "For tests only!")
  fun __getClientAddressToRoomNameCache() = clientAddressToRoomNameCache
  @Deprecated(message = "For tests only!")
  fun __getChatRooms() = chatRooms

  suspend fun createChatRoom(
    isPublic: Boolean = true,
    chatRoomName: String? = null,
    chatRoomPasswordHash: String? = null
  ): ChatRoom {
    val roomName = (chatRoomName ?: SecurityUtils.Generator.generateRandomString(defaultChatRoomLength))
    val chatRoom = ChatRoom(roomName, chatRoomPasswordHash, isPublic, TimeUtils.getCurrentTime())

    mutex.withLock {
      require(chatRooms.size < Constants.maxUsersInRoomCount)
      require(!chatRooms.containsKey(roomName))

      chatRooms[roomName] = chatRoom
    }

    return chatRoom
  }

  suspend fun joinRoom(
    clientAddress: String,
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

      if (!chatRoom.addUser(UserInRoom(user))) {
        return@withLock null
      }

      clientAddressToRoomNameCache.putIfAbsent(clientAddress, mutableListOf())
      clientAddressToRoomNameCache[clientAddress]!!.add(RoomNameUserNamePair(chatRoomName, user.userName))

      return@withLock chatRoom
    }
  }

  suspend fun leaveRoom(
    clientAddress: String,
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

      clientAddressToRoomNameCache[clientAddress]!!.remove(RoomNameUserNamePair(chatRoomName, user.userName))
      if (clientAddressToRoomNameCache[clientAddress]!!.isEmpty()) {
        clientAddressToRoomNameCache.remove(clientAddress)
      }

      chatRoom.removeUser(user.userName)
      return@withLock true
    }
  }

  suspend fun leaveAllRooms(clientAddress: String): List<RoomNameUserNamePair> {
    return mutex.withLock {
      val list = mutableListOf<RoomNameUserNamePair>()

      if (clientAddressToRoomNameCache[clientAddress] == null) {
        return@withLock list
      }

      if (clientAddressToRoomNameCache[clientAddress]!!.isEmpty()) {
        clientAddressToRoomNameCache.remove(clientAddress)
        return@withLock list
      }

      for ((roomName, userName) in clientAddressToRoomNameCache[clientAddress]!!) {
        chatRooms[roomName]?.removeUser(userName)
        list += RoomNameUserNamePair(roomName, userName)
      }

      clientAddressToRoomNameCache.remove(clientAddress)
      return@withLock list
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
          return@map PublicChatRoom(copy.chatRoomName, copy.countUsers().toShort())
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
      return@withLock chatRooms[roomName]
    }
  }

  suspend fun getUser(roomName: String, userName: String): UserInRoom? {
    return mutex.withLock {
      return@withLock chatRooms[roomName]?.getUser(userName)
    }
  }

  data class RoomNameUserNamePair(
    val roomName: String,
    val userName: String
  )
}