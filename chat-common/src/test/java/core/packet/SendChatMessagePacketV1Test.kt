package core.packet

import core.Constants
import core.exception.PacketDeserializationException
import core.security.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class SendChatMessagePacketV1Test : BasePacketPayloadTest() {

  @Test
  fun testPacket() {
    val clientMessageId = 1
    val roomName = "test room"
    val userName = "test user"
    val message = "test message"

    testPayload(SendChatMessagePacket(clientMessageId, roomName, userName, message), { byteSink ->
      SendChatMessagePacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(clientMessageId, restoredPacket.clientMessageId)
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
      assertEquals(0, restoredPacket.clientMessageId)
      assertEquals("", restoredPacket.roomName)
      assertEquals("", restoredPacket.userName)
      assertEquals("", restoredPacket.message)
    })
  }

  @Test(expected = PacketDeserializationException::class)
  fun testPacketExceedRoomNameSize() {
    val clientMessageId = 1
    val roomName = SecurityUtils.Generator.generateRandomString(Constants.maxChatRoomNameLength + 10)
    val userName = "test user"
    val message = "test message"

    testPayload(SendChatMessagePacket(clientMessageId, roomName, userName, message), { byteSink ->
      SendChatMessagePacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(clientMessageId, restoredPacket.clientMessageId)
      assertEquals(roomName, restoredPacket.roomName)
      assertEquals(userName, restoredPacket.userName)
      assertEquals(message, restoredPacket.message)
    })
  }

  @Test(expected = PacketDeserializationException::class)
  fun testPacketExceedUserNameSize() {
    val clientMessageId = 1
    val roomName = "test room"
    val userName = SecurityUtils.Generator.generateRandomString(Constants.maxUserNameLen + 10)
    val message = "test message"

    testPayload(SendChatMessagePacket(clientMessageId, roomName, userName, message), { byteSink ->
      SendChatMessagePacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(clientMessageId, restoredPacket.clientMessageId)
      assertEquals(roomName, restoredPacket.roomName)
      assertEquals(userName, restoredPacket.userName)
      assertEquals(message, restoredPacket.message)
    })
  }

  @Test(expected = PacketDeserializationException::class)
  fun testPacketExceedMessageSize() {
    val clientMessageId = 1
    val roomName = "test room"
    val userName =  "test user"
    val message = SecurityUtils.Generator.generateRandomString(Constants.maxTextMessageLen + 10)

    testPayload(SendChatMessagePacket(clientMessageId, roomName, userName, message), { byteSink ->
      SendChatMessagePacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(clientMessageId, restoredPacket.clientMessageId)
      assertEquals(roomName, restoredPacket.roomName)
      assertEquals(userName, restoredPacket.userName)
      assertEquals(message, restoredPacket.message)
    })
  }
}