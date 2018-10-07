package core.response

import core.Status
import core.model.drainable.PublicUserInChat
import core.model.drainable.chat_message.BaseChatMessage
import core.model.drainable.chat_message.TextChatMessage
import core.security.SecurityUtils
import org.junit.Assert.*
import org.junit.Test

class JoinChatRoomResponsePayloadTest : BaseResponsePayloadTest() {

  @Test
  fun testResponseSuccess() {
    val roomName = "121314"
    val messageHistory = listOf<BaseChatMessage>(
      TextChatMessage(0, "ttt", "wwwwwwwwwwwwwwwwwwwwww"),
      TextChatMessage(1, "se46se46", "wwwwwwwwwwwwwwwwwwwwww"),
      TextChatMessage(2, "6ase46", "wwwwwwwwwwwwwwwwwwwwww"),
      TextChatMessage(3, "hhhhhhhhhhhhhhhhhh", "wwwwwwwwwwwwwwwwwwwwww")
    )
    val usersInChatRoom = listOf(
      PublicUserInChat("test1", ByteArray(277) { 0xAA.toByte() })
    )

    testPayload(JoinChatRoomResponsePayload.success(roomName, messageHistory, usersInChatRoom), { byteSink ->
      JoinChatRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(Status.Ok.value, restoredResponse.status.value)
      assertEquals(roomName, restoredResponse.roomName)

      for (i in 0 until messageHistory.size) {
        val expectedMessage = messageHistory[i] as TextChatMessage
        val actualMessage = restoredResponse.messageHistory[i] as TextChatMessage

        assertEquals(expectedMessage.messageId, actualMessage.messageId)
        assertEquals(expectedMessage.senderName, actualMessage.senderName)
        assertEquals(expectedMessage.message, actualMessage.message)
      }

      for (i in 0 until usersInChatRoom.size) {
        assertEquals(usersInChatRoom[i].userName, restoredResponse.users[i].userName)
        assertArrayEquals(usersInChatRoom[i].ecPublicKey, restoredResponse.users[i].ecPublicKey)
      }
    })
  }

  @Test
  fun testResponseFail() {
    val status = Status.UnknownError

    testPayload(JoinChatRoomResponsePayload.fail(status), { byteSink ->
      JoinChatRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(status.value, restoredResponse.status.value)
    })
  }

  @Test
  fun testResponseEmpty() {
    val roomName = "test"
    val messageHistory = emptyList<BaseChatMessage>()
    val usersInChatRoom = emptyList<PublicUserInChat>()

    testPayload(JoinChatRoomResponsePayload.success(roomName, messageHistory, usersInChatRoom), { byteSink ->
      JoinChatRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(Status.Ok.value, restoredResponse.status.value)
      assertEquals(roomName, restoredResponse.roomName)

      for (i in 0 until messageHistory.size) {
        val expectedMessage = messageHistory[i] as TextChatMessage
        val actualMessage = restoredResponse.messageHistory[i] as TextChatMessage

        assertEquals(expectedMessage.messageId, actualMessage.messageId)
        assertEquals(expectedMessage.senderName, actualMessage.senderName)
        assertEquals(expectedMessage.message, actualMessage.message)
      }

      for (i in 0 until usersInChatRoom.size) {
        assertEquals(usersInChatRoom[i].userName, restoredResponse.users[i].userName)
        assertArrayEquals(usersInChatRoom[i].ecPublicKey, restoredResponse.users[i].ecPublicKey)
      }
    })
  }

  @Test
  fun testResponseWithBigMessageHistory() {
    val roomName = "121314"
    val messageHistory = mutableListOf<BaseChatMessage>()
    val usersInChatRoom = listOf(
      PublicUserInChat("test1", ByteArray(277) { 0xAA.toByte() })
    )

    for (i in 0 until 100) {
      val message = SecurityUtils.Generator.generateRandomString(1024)
      messageHistory += TextChatMessage(i, "test sender", message)
    }

    testPayload(JoinChatRoomResponsePayload.success(roomName, messageHistory, usersInChatRoom), { byteSink ->
      JoinChatRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(Status.Ok.value, restoredResponse.status.value)
      assertEquals(roomName, restoredResponse.roomName)

      for (i in 0 until messageHistory.size) {
        val expectedMessage = messageHistory[i] as TextChatMessage
        val actualMessage = restoredResponse.messageHistory[i] as TextChatMessage

        assertEquals(expectedMessage.messageId, actualMessage.messageId)
        assertEquals(expectedMessage.senderName, actualMessage.senderName)
        assertEquals(expectedMessage.message, actualMessage.message)
      }

      for (i in 0 until usersInChatRoom.size) {
        assertEquals(usersInChatRoom[i].userName, restoredResponse.users[i].userName)
        assertArrayEquals(usersInChatRoom[i].ecPublicKey, restoredResponse.users[i].ecPublicKey)
      }
    })
  }
}