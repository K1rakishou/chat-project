package manager

import core.ChatRoom
import core.Constants
import core.User
import core.UserInRoom
import core.model.drainable.PublicChatRoom
import core.utils.TimeUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlin.coroutines.CoroutineContext

class ChatRoomManager : CoroutineScope {
  private val job = Job()
  private val chatRoomManagerActor: SendChannel<ActorAction>

  override val coroutineContext: CoroutineContext
    get() = job + Dispatchers.Default

  init {
    chatRoomManagerActor = actor(capacity = Channel.UNLIMITED) {
      val userJoinedRooms = mutableMapOf<String, MutableList<RoomNameUserNamePair>>()
      val chatRooms = mutableMapOf<String, ChatRoom>()

      consumeEach { action ->
        when (action) {
          is ActorAction.CreateChatRoom -> {
            val chatRoom = createChatRoomInternal(chatRooms, action.isPublic, action.chatRoomName,
              action.chatRoomImageUrl, action.chatRoomPasswordHash)

            action.result.complete(chatRoom)
          }
          is ActorAction.DoesPasswordMatch -> {
            val match = passwordsMatchInternal(chatRooms, action.chatRoomName, action.chatRoomPassword)
            action.result.complete(match)
          }
          is ActorAction.GetAllPublicRooms -> {
            val publicChatRooms = getAllPublicRoomsInternal(chatRooms)
            action.result.complete(publicChatRooms)
          }
          is ActorAction.GetRoom -> {
            val chatRoom = getChatRoomInternal(chatRooms, action.roomName)
            action.result.complete(chatRoom)
          }
          is ActorAction.GetUser -> {
            val user = getUserInternal(chatRooms, userJoinedRooms, action.clientId, action.roomName, action.userName)
            action.result.complete(user)
          }
          is ActorAction.JoinRoom -> {
            val result = joinRoomInternal(chatRooms, userJoinedRooms, action.clientId, action.chatRoomName, action.user)
            action.result.complete(result)
          }
          is ActorAction.LeaveAllRooms -> {
            val rooms = leaveAllRoomsInternal(chatRooms, userJoinedRooms, action.clientId)
            action.result.complete(rooms)
          }
          is ActorAction.LeaveRoom -> {
            val result = leaveRoomInternal(chatRooms, userJoinedRooms, action.clientId, action.chatRoomName, action.user)
            action.result.complete(result)
          }
          is ActorAction.RoomContainsNickname -> {
            val result = roomContainsNicknameInternal(chatRooms, action.chatRoomName, action.userName)
            action.result.complete(result)
          }
          is ActorAction.RoomExists -> {
            val result = roomExistsInternal(chatRooms, action.chatRoomName)
            action.result.complete(result)
          }
          is ActorAction.RoomHasPassword -> {
            val result = roomHasPasswordInternal(chatRooms, action.chatRoomName)
            action.result.complete(result)
          }
          is ActorAction.UserAlreadyJoined -> {
            val result = userAlreadyJoinedInternal(userJoinedRooms, action.clientId, action.chatRoomName, action.userName)
            action.result.complete(result)
          }

          //for tests
          is ActorAction.GetChatRoomsTest -> action.result.complete(chatRooms)
          is ActorAction.GetUserJoinedRoomsTest -> action.result.complete(userJoinedRooms)
        }
      }
    }
  }

  /**
   * Public methods
   * */

  suspend fun createChatRoom(
    isPublic: Boolean = true,
    chatRoomName: String,
    chatRoomImageUrl: String,
    chatRoomPasswordHash: String? = null
  ): ChatRoom {
    val result = CompletableDeferred<ChatRoom>()
    chatRoomManagerActor.send(ActorAction.CreateChatRoom(result, isPublic, chatRoomName, chatRoomImageUrl, chatRoomPasswordHash))
    return result.await()
  }

  suspend fun joinRoom(
    clientId: String,
    chatRoomName: String,
    user: User
  ): ChatRoom? {
    val result = CompletableDeferred<ChatRoom?>()
    chatRoomManagerActor.send(ActorAction.JoinRoom(result, clientId, chatRoomName, user))
    return result.await()
  }

  suspend fun leaveRoom(
    clientId: String,
    chatRoomName: String,
    user: User
  ): Boolean {
    val result = CompletableDeferred<Boolean>()
    chatRoomManagerActor.send(ActorAction.LeaveRoom(result, clientId, chatRoomName, user))
    return result.await()
  }

  suspend fun leaveAllRooms(
    clientId: String
  ): List<RoomNameUserNamePair> {
    val result = CompletableDeferred<List<RoomNameUserNamePair>>()
    chatRoomManagerActor.send(ActorAction.LeaveAllRooms(result, clientId))
    return result.await()
  }

  suspend fun userAlreadyJoinedRoom(
    clientId: String,
    chatRoomName: String,
    userName: String
  ): Boolean {
    val result = CompletableDeferred<Boolean>()
    chatRoomManagerActor.send(ActorAction.UserAlreadyJoined(result, clientId, chatRoomName, userName))
    return result.await()
  }

  suspend fun roomContainsNickname(
    chatRoomName: String,
    userName: String
  ): Boolean {
    val result = CompletableDeferred<Boolean>()
    chatRoomManagerActor.send(ActorAction.RoomContainsNickname(result, chatRoomName, userName))
    return result.await()
  }

  suspend fun roomExists(
    chatRoomName: String
  ): Boolean {
    val result = CompletableDeferred<Boolean>()
    chatRoomManagerActor.send(ActorAction.RoomExists(result, chatRoomName))
    return result.await()
  }

  suspend fun roomHasPassword(
    chatRoomName: String
  ): Boolean {
    val result = CompletableDeferred<Boolean>()
    chatRoomManagerActor.send(ActorAction.RoomHasPassword(result, chatRoomName))
    return result.await()
  }

  suspend fun getAllPublicRooms(): List<PublicChatRoom> {
    val result = CompletableDeferred<List<PublicChatRoom>>()
    chatRoomManagerActor.send(ActorAction.GetAllPublicRooms(result))
    return result.await()
  }

  suspend fun passwordsMatch(
    chatRoomName: String,
    chatRoomPassword: String
  ): Boolean {
    val result = CompletableDeferred<Boolean>()
    chatRoomManagerActor.send(ActorAction.DoesPasswordMatch(result, chatRoomName, chatRoomPassword))
    return result.await()
  }

  suspend fun getChatRoom(chatRoomName: String): ChatRoom? {
    val result = CompletableDeferred<ChatRoom?>()
    chatRoomManagerActor.send(ActorAction.GetRoom(result, chatRoomName))
    return result.await()
  }

  suspend fun getUser(
    clientId: String,
    roomName: String,
    userName: String
  ): UserInRoom? {
    val result = CompletableDeferred<UserInRoom?>()
    chatRoomManagerActor.send(ActorAction.GetUser(result, clientId, roomName, userName))
    return result.await()
  }

  //for tests only!
  suspend fun __getUserJoinedRooms(): MutableMap<String, MutableList<RoomNameUserNamePair>> {
    val result = CompletableDeferred<MutableMap<String, MutableList<RoomNameUserNamePair>>>()
    chatRoomManagerActor.send(ActorAction.GetUserJoinedRoomsTest(result))
    return result.await()
  }

  //for tests only!
  suspend fun __getChatRooms(): MutableMap<String, ChatRoom> {
    val result = CompletableDeferred<MutableMap<String, ChatRoom>>()
    chatRoomManagerActor.send(ActorAction.GetChatRoomsTest(result))
    return result.await()
  }

  /**
   * Internal methods
   * */

  private fun createChatRoomInternal(
    chatRooms: MutableMap<String, ChatRoom>,
    isPublic: Boolean = true,
    chatRoomName: String,
    chatRoomImageUrl: String,
    chatRoomPasswordHash: String? = null
  ): ChatRoom {
    val chatRoom = ChatRoom(chatRoomName, chatRoomImageUrl, chatRoomPasswordHash, isPublic, TimeUtils.getCurrentTime())

    require(chatRooms.size < Constants.maxUsersInRoomCount)
    require(!chatRooms.containsKey(chatRoomName))

    chatRooms[chatRoomName] = chatRoom
    return chatRoom
  }

  private fun joinRoomInternal(
    chatRooms: MutableMap<String, ChatRoom>,
    userJoinedRooms: MutableMap<String, MutableList<RoomNameUserNamePair>>,
    clientId: String,
    chatRoomName: String,
    user: User
  ): ChatRoom? {
    if (!chatRooms.containsKey(chatRoomName)) {
      return null
    }

    val chatRoom = chatRooms[chatRoomName]!!
    if (chatRoom.containsUser(user.userName)) {
      return null
    }

    if (!chatRoom.addUser(UserInRoom(user))) {
      return null
    }

    userJoinedRooms.putIfAbsent(clientId, mutableListOf())
    userJoinedRooms[clientId]!!.add(RoomNameUserNamePair(chatRoomName, user.userName))

    return chatRoom
  }

  private fun leaveRoomInternal(
    chatRooms: MutableMap<String, ChatRoom>,
    userJoinedRooms: MutableMap<String, MutableList<RoomNameUserNamePair>>,
    clientId: String,
    chatRoomName: String,
    user: User
  ): Boolean {
    if (!chatRooms.containsKey(chatRoomName)) {
      return false
    }

    val chatRoom = chatRooms[chatRoomName]!!
    if (!chatRoom.containsUser(user.userName)) {
      return false
    }

    userJoinedRooms[clientId]!!.remove(RoomNameUserNamePair(chatRoomName, user.userName))
    if (userJoinedRooms[clientId]!!.isEmpty()) {
      userJoinedRooms.remove(clientId)
    }

    chatRoom.removeUser(user.userName)
    return true
  }

  private fun leaveAllRoomsInternal(
    chatRooms: MutableMap<String, ChatRoom>,
    userJoinedRooms: MutableMap<String, MutableList<RoomNameUserNamePair>>,
    clientId: String
  ): List<RoomNameUserNamePair> {
    val list = mutableListOf<RoomNameUserNamePair>()

    if (userJoinedRooms[clientId] == null) {
      return list
    }

    if (userJoinedRooms[clientId]!!.isEmpty()) {
      userJoinedRooms.remove(clientId)
      return list
    }

    for ((roomName, userName) in userJoinedRooms[clientId]!!) {
      chatRooms[roomName]?.removeUser(userName)
      list += RoomNameUserNamePair(roomName, userName)
    }

    userJoinedRooms.remove(clientId)
    return list
  }

  private fun userAlreadyJoinedInternal(
    userJoinedRooms: MutableMap<String, MutableList<RoomNameUserNamePair>>,
    clientId: String,
    chatRoomName: String,
    userName: String
  ): Boolean {
    val cacheEntry = userJoinedRooms[clientId]
    if (cacheEntry == null) {
      return false
    }

    return cacheEntry.any { it.roomName == chatRoomName && it.userName == userName }
  }

  private fun roomContainsNicknameInternal(
    chatRooms: MutableMap<String, ChatRoom>,
    chatRoomName: String,
    userName: String
  ): Boolean {
    require(chatRooms.containsKey(chatRoomName))
    return chatRooms[chatRoomName]!!.containsUser(userName)
  }

  private fun roomExistsInternal(
    chatRooms: MutableMap<String, ChatRoom>,
    chatRoomName: String
  ): Boolean {
    requireNotNull(chatRoomName)
    return chatRooms.containsKey(chatRoomName)
  }

  private fun roomHasPasswordInternal(
    chatRooms: MutableMap<String, ChatRoom>,
    chatRoomName: String
  ): Boolean {
    require(chatRooms.containsKey(chatRoomName))
    return chatRooms[chatRoomName]!!.hasPassword()
  }

  private fun getAllPublicRoomsInternal(
    chatRooms: MutableMap<String, ChatRoom>
  ): List<PublicChatRoom> {
    return chatRooms.values
      .filter { chatRoom -> chatRoom.isPublic }
      .map { chatRoom ->
        val copy = chatRoom.copy()
        return@map PublicChatRoom(copy.chatRoomName, copy.chatRoomImageUrl)
      }
  }

  private fun passwordsMatchInternal(
    chatRooms: MutableMap<String, ChatRoom>,
    chatRoomName: String,
    chatRoomPassword: String
  ): Boolean {
    require(chatRooms.containsKey(chatRoomName))

    return chatRooms[chatRoomName]!!.passwordsMatch(chatRoomPassword)
  }

  private fun getChatRoomInternal(
    chatRooms: MutableMap<String, ChatRoom>,
    roomName: String
  ): ChatRoom? {
    return chatRooms[roomName]
  }

  private fun getUserInternal(
    chatRooms: MutableMap<String, ChatRoom>,
    userJoinedRooms: MutableMap<String, MutableList<RoomNameUserNamePair>>,
    clientId: String,
    roomName: String,
    userName: String
  ): UserInRoom? {
    val joinedRoom = userJoinedRooms[clientId]
      ?.any { it.roomName == roomName && it.userName == userName }
      ?: false

    if (!joinedRoom) {
      return null
    }

    return chatRooms[roomName]?.getUser(userName)
  }

  sealed class ActorAction {
    class CreateChatRoom(val result: CompletableDeferred<ChatRoom>,
                         val isPublic: Boolean = true,
                         val chatRoomName: String,
                         val chatRoomImageUrl: String,
                         val chatRoomPasswordHash: String? = null) : ActorAction()

    class JoinRoom(val result: CompletableDeferred<ChatRoom?>,
                   val clientId: String,
                   val chatRoomName: String,
                   val user: User) : ActorAction()

    class LeaveRoom(val result: CompletableDeferred<Boolean>,
                    val clientId: String,
                    val chatRoomName: String,
                    val user: User) : ActorAction()

    class LeaveAllRooms(val result: CompletableDeferred<List<RoomNameUserNamePair>>,
                        val clientId: String) : ActorAction()

    class UserAlreadyJoined(val result: CompletableDeferred<Boolean>,
                            val clientId: String,
                            val chatRoomName: String,
                            val userName: String) : ActorAction()

    class RoomContainsNickname(val result: CompletableDeferred<Boolean>,
                               val chatRoomName: String,
                               val userName: String) : ActorAction()

    class RoomExists(val result: CompletableDeferred<Boolean>,
                     val chatRoomName: String) : ActorAction()

    class RoomHasPassword(val result: CompletableDeferred<Boolean>,
                          val chatRoomName: String) : ActorAction()

    class GetAllPublicRooms(val result: CompletableDeferred<List<PublicChatRoom>>) : ActorAction()

    class DoesPasswordMatch(val result: CompletableDeferred<Boolean>,
                            val chatRoomName: String,
                            val chatRoomPassword: String) : ActorAction()

    class GetRoom(val result: CompletableDeferred<ChatRoom?>,
                  val roomName: String) : ActorAction()

    class GetUser(val result: CompletableDeferred<UserInRoom?>,
                  val clientId: String,
                  val roomName: String,
                  val userName: String) : ActorAction()

    //for tests

    class GetUserJoinedRoomsTest(
      val result: CompletableDeferred<MutableMap<String, MutableList<RoomNameUserNamePair>>>
    ) : ActorAction()

    class GetChatRoomsTest(
      val result: CompletableDeferred<MutableMap<String, ChatRoom>>
    ) : ActorAction()
  }

  data class RoomNameUserNamePair(
    val roomName: String,
    val userName: String
  )
}