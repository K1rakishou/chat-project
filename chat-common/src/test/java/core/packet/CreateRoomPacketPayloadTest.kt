package core.packet

import core.exception.ByteSinkReadException
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
    val roomName = "dgfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsgdfgdg" +
      "fdsgddfgfgdgfdsgdfgdgfdsgdfgdg"
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
    val roomPassword = "gfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsg" +
      "ddfgfgdgfdsgdfgdgfdsgdfgdggfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsgdfgdgfdsg" +
      "dfgdgfdsgddfgfgdgfdsgdfgdgfdsgdfgdg"

    testPayload(CreateRoomPacket(isPublic, roomName, roomPassword), { byteSink ->
      CreateRoomPacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(isPublic, restoredPacket.isPublic)
      assertEquals(roomName, restoredPacket.chatRoomName)
      assertEquals(roomPassword, restoredPacket.chatRoomPasswordHash)
    })
  }
}