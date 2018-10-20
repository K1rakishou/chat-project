package core.packet

import core.Constants
import core.exception.PacketDeserializationException
import core.security.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class CreateRoomPacketPayloadV1Test : BasePacketPayloadTest() {

  @Test
  fun testPacket() {
    val isPublic = true
    val roomName = "dgfdsgdfg"
    val roomPassword = "fgfdhd"
    val chatRoomImageUrl = "imgur.com/123.jpg"
    val userName = "test123"

    testPayload(CreateRoomPacket(isPublic, roomName, roomPassword, chatRoomImageUrl, userName), { byteSink ->
      CreateRoomPacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(isPublic, restoredPacket.isPublic)
      assertEquals(roomName, restoredPacket.chatRoomName)
      assertEquals(roomPassword, restoredPacket.chatRoomPasswordHash)
      assertEquals(chatRoomImageUrl, restoredPacket.chatRoomImageUrl)
      assertEquals(userName, restoredPacket.userName)
    })
  }

  @Test(expected = PacketDeserializationException::class)
  fun testPacketNullFields() {
    val isPublic = true
    val roomName = null
    val roomPassword = null
    val chatRoomImageUrl = null
    val userName = null

    testPayload(CreateRoomPacket(isPublic, roomName, roomPassword, chatRoomImageUrl, userName), { byteSink ->
      CreateRoomPacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(isPublic, restoredPacket.isPublic)
      assertEquals(roomName, restoredPacket.chatRoomName)
      assertEquals(roomPassword, restoredPacket.chatRoomPasswordHash)
      assertEquals(chatRoomImageUrl, restoredPacket.chatRoomImageUrl)
      assertEquals(userName, restoredPacket.userName)
    })
  }

  @Test(expected = PacketDeserializationException::class)
  fun testPacketExceedMaxRoomNameLen() {
    val isPublic = true
    val roomName = SecurityUtils.Generator.generateRandomString(Constants.maxChatRoomNameLength + 10)
    val roomPassword = "fgfdhd"
    val chatRoomImageUrl = "imgur.com/123.jpg"
    val userName = "test123"

    testPayload(CreateRoomPacket(isPublic, roomName, roomPassword, chatRoomImageUrl, userName), { byteSink ->
      CreateRoomPacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(isPublic, restoredPacket.isPublic)
      assertEquals(roomName, restoredPacket.chatRoomName)
      assertEquals(roomPassword, restoredPacket.chatRoomPasswordHash)
      assertEquals(chatRoomImageUrl, restoredPacket.chatRoomImageUrl)
      assertEquals(userName, restoredPacket.userName)
    })
  }

  @Test(expected = PacketDeserializationException::class)
  fun testPacketExceedMaxRoomPasswordLen() {
    val isPublic = true
    val roomName = "fgfdhd"
    val roomPassword = SecurityUtils.Generator.generateRandomString(Constants.maxChatRoomPasswordHashLen + 10)
    val chatRoomImageUrl = "imgur.com/123.jpg"
    val userName = "test123"

    testPayload(CreateRoomPacket(isPublic, roomName, roomPassword, chatRoomImageUrl, userName), { byteSink ->
      CreateRoomPacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(isPublic, restoredPacket.isPublic)
      assertEquals(roomName, restoredPacket.chatRoomName)
      assertEquals(roomPassword, restoredPacket.chatRoomPasswordHash)
      assertEquals(chatRoomImageUrl, restoredPacket.chatRoomImageUrl)
      assertEquals(userName, restoredPacket.userName)
    })
  }

  @Test(expected = PacketDeserializationException::class)
  fun testPacketExceedMaxRoomImageUrlLen() {
    val isPublic = true
    val roomName = "fgfdhd"
    val roomPassword = "fgfdhd"
    val chatRoomImageUrl = SecurityUtils.Generator.generateRandomString(Constants.maxImageUrlLen + 10)
    val userName = "test123"

    testPayload(CreateRoomPacket(isPublic, roomName, roomPassword, chatRoomImageUrl, userName), { byteSink ->
      CreateRoomPacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(isPublic, restoredPacket.isPublic)
      assertEquals(roomName, restoredPacket.chatRoomName)
      assertEquals(roomPassword, restoredPacket.chatRoomPasswordHash)
      assertEquals(chatRoomImageUrl, restoredPacket.chatRoomImageUrl)
      assertEquals(userName, restoredPacket.userName)
    })
  }

  @Test(expected = PacketDeserializationException::class)
  fun testPacketExceedMaxUserNameLen() {
    val isPublic = true
    val roomName = "fgfdhd"
    val roomPassword = "fgfdhd"
    val chatRoomImageUrl = "imgur.com/123.jpg"
    val userName = SecurityUtils.Generator.generateRandomString(Constants.maxUserNameLen + 10)

    testPayload(CreateRoomPacket(isPublic, roomName, roomPassword, chatRoomImageUrl, userName), { byteSink ->
      CreateRoomPacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(isPublic, restoredPacket.isPublic)
      assertEquals(roomName, restoredPacket.chatRoomName)
      assertEquals(roomPassword, restoredPacket.chatRoomPasswordHash)
      assertEquals(chatRoomImageUrl, restoredPacket.chatRoomImageUrl)
      assertEquals(userName, restoredPacket.userName)
    })
  }
}