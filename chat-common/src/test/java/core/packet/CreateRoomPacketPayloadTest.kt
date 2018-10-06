package core.packet

import core.Constants
import core.exception.ByteSinkReadException
import core.security.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class CreateRoomPacketPayloadTest : BasePacketPayloadTest() {

  @Test
  fun testPacket() {
    val isPublic = true
    val roomName = "dgfdsgdfg"
    val roomPassword = "fgfdhd"

    testPayload(CreateRoomPacket(isPublic, roomName, roomPassword), { byteSink ->
      CreateRoomPacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(isPublic, restoredPacket.isPublic)
      assertEquals(roomName, restoredPacket.chatRoomName)
      assertEquals(roomPassword, restoredPacket.chatRoomPasswordHash)
    })
  }

  @Test
  fun testPacketNullFields() {
    val isPublic = true
    val roomName = null
    val roomPassword = null

    testPayload(CreateRoomPacket(isPublic, roomName, roomPassword), { byteSink ->
      CreateRoomPacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(isPublic, restoredPacket.isPublic)
      assertEquals(roomName, restoredPacket.chatRoomName)
      assertEquals(roomPassword, restoredPacket.chatRoomPasswordHash)
    })
  }

  @Test(expected = ByteSinkReadException::class)
  fun testPacketExceedMaxRoomName() {
    val isPublic = true
    val roomName = SecurityUtils.Generation.generateRandomString(Constants.maxChatRoomNameLength + 10)
    val roomPassword = "fgfdhd"

    testPayload(CreateRoomPacket(isPublic, roomName, roomPassword), { byteSink ->
      CreateRoomPacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(isPublic, restoredPacket.isPublic)
      assertEquals(roomName, restoredPacket.chatRoomName)
      assertEquals(roomPassword, restoredPacket.chatRoomPasswordHash)
    })
  }

  @Test(expected = ByteSinkReadException::class)
  fun testPacketExceedMaxRoomPassword() {
    val isPublic = true
    val roomName = "fgfdhd"
    val roomPassword = SecurityUtils.Generation.generateRandomString(Constants.maxChatRoomPasswordHash + 10)

    testPayload(CreateRoomPacket(isPublic, roomName, roomPassword), { byteSink ->
      CreateRoomPacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(isPublic, restoredPacket.isPublic)
      assertEquals(roomName, restoredPacket.chatRoomName)
      assertEquals(roomPassword, restoredPacket.chatRoomPasswordHash)
    })
  }
}