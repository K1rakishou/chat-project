package manager

import core.ChatRoom
import core.Constants
import core.User
import core.UserInRoom
import core.extensions.myWithLock
import core.model.drainable.PublicChatRoom
import core.security.SecurityUtils
import core.utils.TimeUtils
import kotlinx.coroutines.experimental.sync.Mutex

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
    chatRoomPasswordHash: ByteArray? = null
  ): ChatRoom {
    val roomName = (chatRoomName ?: SecurityUtils.Generator.generateRandomString(defaultChatRoomLength))
    val chatRoom = ChatRoom(roomName, chatRoomPasswordHash, isPublic, TimeUtils.getCurrentTime())

    mutex.myWithLock {
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

      clientAddressToRoomNameCache.putIfAbsent(clientAddress, mutableListOf())
      clientAddressToRoomNameCache[clientAddress]!!.add(RoomNameUserNamePair(chatRoomName, user.userName))

      return@myWithLock chatRoom
    }
  }

  suspend fun leaveRoom(
    clientAddress: String,
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

      clientAddressToRoomNameCache[clientAddress]!!.remove(RoomNameUserNamePair(chatRoomName, user.userName))
      if (clientAddressToRoomNameCache[clientAddress]!!.isEmpty()) {
        clientAddressToRoomNameCache.remove(clientAddress)
      }

      chatRoom.removeUser(user.userName)
      return@myWithLock true
    }
  }

  suspend fun leaveAllRooms(clientAddress: String): List<RoomNameUserNamePair> {
    return mutex.myWithLock {
      val list = mutableListOf<RoomNameUserNamePair>()

      if (clientAddressToRoomNameCache[clientAddress] == null) {
        return@myWithLock list
      }

      if (clientAddressToRoomNameCache[clientAddress]!!.isEmpty()) {
        clientAddressToRoomNameCache.remove(clientAddress)
        return@myWithLock list
      }

      for ((roomName, userName) in clientAddressToRoomNameCache[clientAddress]!!) {
        chatRooms[roomName]?.removeUser(userName)
        list += RoomNameUserNamePair(roomName, userName)
      }

      clientAddressToRoomNameCache.remove(clientAddress)
      return@myWithLock list
    }
  }

  suspend fun alreadyJoined(clientAddress: String, chatRoomName: String, userName: String): Boolean {
    return mutex.myWithLock {
      val cacheEntry = clientAddressToRoomNameCache[clientAddress]
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
          return@map PublicChatRoom(copy.chatRoomName, copy.countUsers().toShort())
        }
    }
  }

  suspend fun passwordsMatch(chatRoomName: String, chatRoomPassword: ByteArray): Boolean {
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

  suspend fun getUser(clientAddress: String, roomName: String, userName: String): UserInRoom? {
    return mutex.myWithLock {
      val joinedRoom = clientAddressToRoomNameCache[clientAddress]?.any { it.roomName == roomName && it.userName == userName } ?: false
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