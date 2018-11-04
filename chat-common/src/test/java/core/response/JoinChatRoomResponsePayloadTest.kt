package core.response

import core.Constants
import core.Status
import core.exception.ResponseDeserializationException
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
    val userName = "test"
    val messageHistory = listOf<BaseChatMessage>(
      TextChatMessage(false, 0, 0, "ttt", "wwwwwwwwwwwwwwwwwwwwww"),
      TextChatMessage(true, 1, 1, "se46se46", "wwwwwwwwwwwwwwwwwwwwww"),
      TextChatMessage(false, 2, 2, "6ase46", "wwwwwwwwwwwwwwwwwwwwww"),
      TextChatMessage(true, 3, 3, "hhhhhhhhhhhhhhhhhh", "wwwwwwwwwwwwwwwwwwwwww")
    )
    val usersInChatRoom = listOf(
      PublicUserInChat("test1")
    )

    testPayload(JoinChatRoomResponsePayload.success(roomName, userName, messageHistory, usersInChatRoom), { byteSink ->
      JoinChatRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(Status.Ok.value, restoredResponse.status.value)
      assertEquals(roomName, restoredResponse.roomName)
      assertEquals(userName, restoredResponse.userName)

      for (i in 0 until messageHistory.size) {
        val expectedMessage = messageHistory[i] as TextChatMessage
        val actualMessage = restoredResponse.messageHistory[i] as TextChatMessage

        assertEquals(expectedMessage.serverMessageId, actualMessage.serverMessageId)
        assertEquals(expectedMessage.clientMessageId, actualMessage.clientMessageId)
        assertEquals(expectedMessage.senderName, actualMessage.senderName)
        assertEquals(expectedMessage.message, actualMessage.message)
        assertEquals(expectedMessage.isMyMessage, actualMessage.isMyMessage)
      }

      for (i in 0 until usersInChatRoom.size) {
        assertEquals(usersInChatRoom[i].userName, restoredResponse.users[i].userName)
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
    val userName = "test"
    val messageHistory = emptyList<BaseChatMessage>()
    val usersInChatRoom = emptyList<PublicUserInChat>()

    testPayload(JoinChatRoomResponsePayload.success(roomName, userName, messageHistory, usersInChatRoom), { byteSink ->
      JoinChatRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(Status.Ok.value, restoredResponse.status.value)
      assertEquals(roomName, restoredResponse.roomName)
      assertEquals(userName, restoredResponse.userName)

      for (i in 0 until messageHistory.size) {
        val expectedMessage = messageHistory[i] as TextChatMessage
        val actualMessage = restoredResponse.messageHistory[i] as TextChatMessage

        assertEquals(expectedMessage.serverMessageId, actualMessage.serverMessageId)
        assertEquals(expectedMessage.clientMessageId, actualMessage.clientMessageId)
        assertEquals(expectedMessage.senderName, actualMessage.senderName)
        assertEquals(expectedMessage.message, actualMessage.message)
        assertEquals(expectedMessage.isMyMessage, actualMessage.isMyMessage)
      }

      for (i in 0 until usersInChatRoom.size) {
        assertEquals(usersInChatRoom[i].userName, restoredResponse.users[i].userName)
      }
    })
  }

  @Test(expected = ResponseDeserializationException::class)
  fun testResponseExceedTextMessageSenderName() {
    val roomName = "121314"
    val userName = "test"
    val messageHistory = listOf<BaseChatMessage>(
      TextChatMessage(false, 0, 1, SecurityUtils.Generator.generateRandomString(Constants.maxUserNameLen + 10), "wwwwwwwwwwwwwwwwwwwwww")
    )
    val usersInChatRoom = listOf(
      PublicUserInChat("test1")
    )

    testPayload(JoinChatRoomResponsePayload.success(roomName, userName, messageHistory, usersInChatRoom), { byteSink ->
      JoinChatRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(Status.Ok.value, restoredResponse.status.value)
      assertEquals(roomName, restoredResponse.roomName)
      assertEquals(userName, restoredResponse.userName)

      for (i in 0 until messageHistory.size) {
        val expectedMessage = messageHistory[i] as TextChatMessage
        val actualMessage = restoredResponse.messageHistory[i] as TextChatMessage

        assertEquals(expectedMessage.serverMessageId, actualMessage.serverMessageId)
        assertEquals(expectedMessage.clientMessageId, actualMessage.clientMessageId)
        assertEquals(expectedMessage.senderName, actualMessage.senderName)
        assertEquals(expectedMessage.message, actualMessage.message)
        assertEquals(expectedMessage.isMyMessage, actualMessage.isMyMessage)
      }

      for (i in 0 until usersInChatRoom.size) {
        assertEquals(usersInChatRoom[i].userName, restoredResponse.users[i].userName)
      }
    })
  }

  @Test(expected = ResponseDeserializationException::class)
  fun testResponseExceedTextMessageMessage() {
    val roomName = "121314"
    val userName = "test"
    val messageHistory = listOf<BaseChatMessage>(
      TextChatMessage(true, 0, 1, "e5we6s76", SecurityUtils.Generator.generateRandomString(Constants.maxTextMessageLen + 10))
    )
    val usersInChatRoom = listOf(
      PublicUserInChat("test1")
    )

    testPayload(JoinChatRoomResponsePayload.success(roomName, userName, messageHistory, usersInChatRoom), { byteSink ->
      JoinChatRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(Status.Ok.value, restoredResponse.status.value)
      assertEquals(roomName, restoredResponse.roomName)
      assertEquals(userName, restoredResponse.userName)

      for (i in 0 until messageHistory.size) {
        val expectedMessage = messageHistory[i] as TextChatMessage
        val actualMessage = restoredResponse.messageHistory[i] as TextChatMessage

        assertEquals(expectedMessage.serverMessageId, actualMessage.serverMessageId)
        assertEquals(expectedMessage.clientMessageId, actualMessage.clientMessageId)
        assertEquals(expectedMessage.senderName, actualMessage.senderName)
        assertEquals(expectedMessage.message, actualMessage.message)
        assertEquals(expectedMessage.isMyMessage, actualMessage.isMyMessage)
      }

      for (i in 0 until usersInChatRoom.size) {
        assertEquals(usersInChatRoom[i].userName, restoredResponse.users[i].userName)
      }
    })
  }

  @Test(expected = ResponseDeserializationException::class)
  fun testResponseExceedUserInChatUserName() {
    val roomName = "121314"
    val userName = "test"
    val messageHistory = listOf<BaseChatMessage>(
      TextChatMessage(false, 0, 1, "e5we6s76", "45346347")
    )
    val usersInChatRoom = listOf(
      PublicUserInChat(SecurityUtils.Generator.generateRandomString(Constants.maxUserNameLen + 10))
    )

    testPayload(JoinChatRoomResponsePayload.success(roomName, userName, messageHistory, usersInChatRoom), { byteSink ->
      JoinChatRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(Status.Ok.value, restoredResponse.status.value)
      assertEquals(roomName, restoredResponse.roomName)
      assertEquals(userName, restoredResponse.userName)

      for (i in 0 until messageHistory.size) {
        val expectedMessage = messageHistory[i] as TextChatMessage
        val actualMessage = restoredResponse.messageHistory[i] as TextChatMessage

        assertEquals(expectedMessage.serverMessageId, actualMessage.serverMessageId)
        assertEquals(expectedMessage.clientMessageId, actualMessage.clientMessageId)
        assertEquals(expectedMessage.senderName, actualMessage.senderName)
        assertEquals(expectedMessage.message, actualMessage.message)
        assertEquals(expectedMessage.isMyMessage, actualMessage.isMyMessage)
      }

      for (i in 0 until usersInChatRoom.size) {
        assertEquals(usersInChatRoom[i].userName, restoredResponse.users[i].userName)
      }
    })
  }

  @Test(expected = ResponseDeserializationException::class)
  fun testResponseExceedUserName() {
    val roomName = "121314"
    val userName = SecurityUtils.Generator.generateRandomString(Constants.maxUserNameLen + 10)
    val messageHistory = listOf<BaseChatMessage>(
      TextChatMessage(true, 0, 0, "ttt", "wwwwwwwwwwwwwwwwwwwwww"),
      TextChatMessage(true, 1, 1, "se46se46", "wwwwwwwwwwwwwwwwwwwwww"),
      TextChatMessage(false, 2, 2, "6ase46", "wwwwwwwwwwwwwwwwwwwwww"),
      TextChatMessage(false, 3, 3, "hhhhhhhhhhhhhhhhhh", "wwwwwwwwwwwwwwwwwwwwww")
    )
    val usersInChatRoom = listOf(
      PublicUserInChat("test1")
    )

    testPayload(JoinChatRoomResponsePayload.success(roomName, userName, messageHistory, usersInChatRoom), { byteSink ->
      JoinChatRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(Status.Ok.value, restoredResponse.status.value)
      assertEquals(roomName, restoredResponse.roomName)
      assertEquals(userName, restoredResponse.userName)

      for (i in 0 until messageHistory.size) {
        val expectedMessage = messageHistory[i] as TextChatMessage
        val actualMessage = restoredResponse.messageHistory[i] as TextChatMessage

        assertEquals(expectedMessage.serverMessageId, actualMessage.serverMessageId)
        assertEquals(expectedMessage.clientMessageId, actualMessage.clientMessageId)
        assertEquals(expectedMessage.senderName, actualMessage.senderName)
        assertEquals(expectedMessage.message, actualMessage.message)
        assertEquals(expectedMessage.isMyMessage, actualMessage.isMyMessage)
      }

      for (i in 0 until usersInChatRoom.size) {
        assertEquals(usersInChatRoom[i].userName, restoredResponse.users[i].userName)
      }
    })
  }

  @Test
  fun testResponseWithBigMessageHistory() {
    val roomName = "121314"
    val userName = "test"
    val messageHistory = mutableListOf<BaseChatMessage>()
    val usersInChatRoom = listOf(
      PublicUserInChat("test1")
    )

    for (i in 0 until 100) {
      val message = SecurityUtils.Generator.generateRandomString(1024)
      messageHistory += TextChatMessage(false, i, i, "test sender", message)
    }

    testPayload(JoinChatRoomResponsePayload.success(roomName, userName, messageHistory, usersInChatRoom), { byteSink ->
      JoinChatRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(Status.Ok.value, restoredResponse.status.value)
      assertEquals(roomName, restoredResponse.roomName)
      assertEquals(userName, restoredResponse.userName)

      for (i in 0 until messageHistory.size) {
        val expectedMessage = messageHistory[i] as TextChatMessage
        val actualMessage = restoredResponse.messageHistory[i] as TextChatMessage

        assertEquals(expectedMessage.serverMessageId, actualMessage.serverMessageId)
        assertEquals(expectedMessage.clientMessageId, actualMessage.clientMessageId)
        assertEquals(expectedMessage.senderName, actualMessage.senderName)
        assertEquals(expectedMessage.message, actualMessage.message)
        assertEquals(expectedMessage.isMyMessage, actualMessage.isMyMessage)
      }

      for (i in 0 until usersInChatRoom.size) {
        assertEquals(usersInChatRoom[i].userName, restoredResponse.users[i].userName)
      }
    })
  }
}