package manager

import core.User
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ChatRoomManagerTest {
  val chatRoomName = "test room"
  val chatRoomName2 = "test room2"

  val chatRoomImageUrl = "imgur.com/123.jpg"
  val chatRoomImageUrl2 = "imgur.com/234.jpg"

  val clientAddress1 = "127.0.0.1"
  val userName1 = "test user"
  val user1 = User(userName1, clientAddress1)

  val clientAddress2 = "127.0.0.2"
  val userName2 = "test user2"
  val user2 = User(userName2, clientAddress2)

  val chatRoomManager = ChatRoomManager()

  @Test
  fun `should add user to the chatRoom and to the user cache`() {
    runBlocking {
      kotlin.run {
        chatRoomManager.createChatRoom(chatRoomName = chatRoomName, chatRoomImageUrl = chatRoomImageUrl)

        assertNotNull(chatRoomManager.joinRoom(clientAddress1, chatRoomName, user1))

        val userInRoom = chatRoomManager.__getChatRooms()[chatRoomName]!!.getUser(userName1)
        assertNotNull(user1)

        assertEquals(userName1, userInRoom!!.user.userName)
        assertEquals(clientAddress1, userInRoom.user.clientAddress)

        val roomsJoinedByUser = chatRoomManager.__getClientAddressToRoomNameCache()[clientAddress1]
        assertNotNull(roomsJoinedByUser)

        assertEquals(1, roomsJoinedByUser!!.size)
        assertEquals(chatRoomName, roomsJoinedByUser[0].roomName)
        assertEquals(userName1, roomsJoinedByUser[0].userName)
      }

      kotlin.run {
        assertNotNull(chatRoomManager.joinRoom(clientAddress2, chatRoomName, user2))

        val userInRoom = chatRoomManager.__getChatRooms()[chatRoomName]!!.getUser(userName2)
        assertNotNull(user2)

        assertEquals(userName2, userInRoom!!.user.userName)
        assertEquals(clientAddress2, userInRoom.user.clientAddress)

        val roomsJoinedByUser = chatRoomManager.__getClientAddressToRoomNameCache()[clientAddress2]
        assertNotNull(roomsJoinedByUser)

        assertEquals(1, roomsJoinedByUser!!.size)
        assertEquals(chatRoomName, roomsJoinedByUser[0].roomName)
        assertEquals(userName2, roomsJoinedByUser[0].userName)
      }
    }
  }

  @Test
  fun `should not be able to join non-existing room`() {
    runBlocking {
      assertNull(chatRoomManager.joinRoom(clientAddress1, "non existing room", user1))
      assertNull(chatRoomManager.__getChatRooms()["non existing room"])
      assertTrue(chatRoomManager.__getClientAddressToRoomNameCache().isEmpty())
    }
  }

  @Test
  fun `should not be able to join one room twice`() {
    runBlocking {
      chatRoomManager.createChatRoom(chatRoomName = chatRoomName, chatRoomImageUrl = chatRoomImageUrl)

      assertNotNull(chatRoomManager.joinRoom(clientAddress1, chatRoomName, user1))
      assertNull(chatRoomManager.joinRoom(clientAddress1, chatRoomName, user1))

      assertTrue(chatRoomManager.__getChatRooms()[chatRoomName]!!.containsUser(userName1))
      assertEquals(1, chatRoomManager.__getClientAddressToRoomNameCache().size)
    }
  }

  @Test
  fun `should remove user from the room and from the cache`() {
    runBlocking {
      chatRoomManager.createChatRoom(chatRoomName = chatRoomName, chatRoomImageUrl = chatRoomImageUrl)

      assertNotNull(chatRoomManager.joinRoom(clientAddress1, chatRoomName, user1))
      assertNotNull(chatRoomManager.joinRoom(clientAddress2, chatRoomName, user2))

      assertTrue(chatRoomManager.leaveRoom(clientAddress1, chatRoomName, user1))
      assertFalse(chatRoomManager.__getChatRooms()[chatRoomName]!!.containsUser(userName1))
      assertNull(chatRoomManager.__getClientAddressToRoomNameCache()[clientAddress1])

      assertTrue(chatRoomManager.__getChatRooms()[chatRoomName]!!.containsUser(userName2))

      assertTrue(chatRoomManager.leaveRoom(clientAddress2, chatRoomName, user2))
      assertFalse(chatRoomManager.__getChatRooms()[chatRoomName]!!.containsUser(userName2))
      assertNull(chatRoomManager.__getClientAddressToRoomNameCache()[clientAddress2])

      assertTrue(chatRoomManager.__getChatRooms()[chatRoomName]!!.userList.isEmpty())
      assertTrue(chatRoomManager.__getClientAddressToRoomNameCache().isEmpty())
    }
  }
}