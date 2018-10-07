package core.packet

import core.Constants
import core.exception.ByteSinkReadException
import core.security.SecurityUtils
import org.junit.Assert.*
import org.junit.Test

class SendChatMessagePacketTest : BasePacketPayloadTest() {

  @Test
  fun testPacket() {
    val messageId = 1
    val roomName = "test room"
    val userName = "test user"
    val message = "test message"

    testPayload(SendChatMessagePacket(messageId, roomName, userName, message), { byteSink ->
      SendChatMessagePacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(messageId, restoredPacket.messageId)
      assertEquals(roomName, restoredPacket.roomName)
      assertEquals(userName, restoredPacket.userName)
      assertEquals(message, restoredPacket.message)
    })
  }

  @Test
  fun testEmptyPacket() {
    testPayload(SendChatMessagePacket(0, "", "", ""), { byteSink ->
      SendChatMessagePacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(0, restoredPacket.messageId)
      assertEquals("", restoredPacket.roomName)
      assertEquals("", restoredPacket.userName)
      assertEquals("", restoredPacket.message)
    })
  }

  @Test(expected = ByteSinkReadException::class)
  fun testPacketExceedRoomNameSize() {
    val messageId = 1
    val roomName = SecurityUtils.Generation.generateRandomString(Constants.maxChatRoomNameLength + 10)
    val userName = "test user"
    val message = "test message"

    testPayload(SendChatMessagePacket(messageId, roomName, userName, message), { byteSink ->
      SendChatMessagePacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(messageId, restoredPacket.messageId)
      assertEquals(roomName, restoredPacket.roomName)
      assertEquals(userName, restoredPacket.userName)
      assertEquals(message, restoredPacket.message)
    })
  }

  @Test(expected = ByteSinkReadException::class)
  fun testPacketExceedUserNameSize() {
    val messageId = 1
    val roomName = "test room"
    val userName = SecurityUtils.Generation.generateRandomString(Constants.maxUserNameLen + 10)
    val message = "test message"

    testPayload(SendChatMessagePacket(messageId, roomName, userName, message), { byteSink ->
      SendChatMessagePacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(messageId, restoredPacket.messageId)
      assertEquals(roomName, restoredPacket.roomName)
      assertEquals(userName, restoredPacket.userName)
      assertEquals(message, restoredPacket.message)
    })
  }

  @Test(expected = ByteSinkReadException::class)
  fun testPacketExceedMessageSize() {
    val messageId = 1
    val roomName = "test room"
    val userName =  "test user"
    val message = SecurityUtils.Generation.generateRandomString(Constants.maxTextMessageLen + 10)

    testPayload(SendChatMessagePacket(messageId, roomName, userName, message), { byteSink ->
      SendChatMessagePacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(messageId, restoredPacket.messageId)
      assertEquals(roomName, restoredPacket.roomName)
      assertEquals(userName, restoredPacket.userName)
      assertEquals(message, restoredPacket.message)
    })
  }
}