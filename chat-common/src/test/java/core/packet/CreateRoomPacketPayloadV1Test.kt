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

    testPayload(CreateRoomPacket(isPublic, roomName, roomPassword, chatRoomImageUrl), { byteSink ->
      CreateRoomPacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(isPublic, restoredPacket.isPublic)
      assertEquals(roomName, restoredPacket.chatRoomName)
      assertEquals(roomPassword, restoredPacket.chatRoomPasswordHash)
    })
  }

  @Test(expected = PacketDeserializationException::class)
  fun testPacketNullFields() {
    val isPublic = true
    val roomName = null
    val roomPassword = null
    val chatRoomImageUrl = null

    testPayload(CreateRoomPacket(isPublic, roomName, roomPassword, chatRoomImageUrl), { byteSink ->
      CreateRoomPacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(isPublic, restoredPacket.isPublic)
      assertEquals(roomName, restoredPacket.chatRoomName)
      assertEquals(roomPassword, restoredPacket.chatRoomPasswordHash)
    })
  }

  @Test(expected = PacketDeserializationException::class)
  fun testPacketExceedMaxRoomName() {
    val isPublic = true
    val roomName = SecurityUtils.Generator.generateRandomString(Constants.maxChatRoomNameLength + 10)
    val roomPassword = "fgfdhd"
    val chatRoomImageUrl = "imgur.com/123.jpg"

    testPayload(CreateRoomPacket(isPublic, roomName, roomPassword, chatRoomImageUrl), { byteSink ->
      CreateRoomPacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(isPublic, restoredPacket.isPublic)
      assertEquals(roomName, restoredPacket.chatRoomName)
      assertEquals(roomPassword, restoredPacket.chatRoomPasswordHash)
    })
  }

  @Test(expected = PacketDeserializationException::class)
  fun testPacketExceedMaxRoomPassword() {
    val isPublic = true
    val roomName = "fgfdhd"
    val roomPassword = SecurityUtils.Generator.generateRandomString(Constants.maxChatRoomPasswordHashLen + 10)
    val chatRoomImageUrl = "imgur.com/123.jpg"

    testPayload(CreateRoomPacket(isPublic, roomName, roomPassword, chatRoomImageUrl), { byteSink ->
      CreateRoomPacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(isPublic, restoredPacket.isPublic)
      assertEquals(roomName, restoredPacket.chatRoomName)
      assertEquals(roomPassword, restoredPacket.chatRoomPasswordHash)
    })
  }

  @Test(expected = PacketDeserializationException::class)
  fun testPacketExceedMaxRoomImageUrl() {
    val isPublic = true
    val roomName = "fgfdhd"
    val roomPassword = "fgfdhd"
    val chatRoomImageUrl = SecurityUtils.Generator.generateRandomString(Constants.maxImageUrlLen + 10)

    testPayload(CreateRoomPacket(isPublic, roomName, roomPassword, chatRoomImageUrl), { byteSink ->
      CreateRoomPacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(isPublic, restoredPacket.isPublic)
      assertEquals(roomName, restoredPacket.chatRoomName)
      assertEquals(roomPassword, restoredPacket.chatRoomPasswordHash)
    })
  }
}