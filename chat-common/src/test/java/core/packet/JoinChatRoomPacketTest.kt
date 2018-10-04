package core.packet

import org.junit.Assert.*
import org.junit.Test

class JoinChatRoomPacketTest : BasePacketPayloadTest() {

  @Test
  fun testPacket() {
    val ecPublicKey = ByteArray(555) { 0x66.toByte() }
    val userName = "test_user_name"
    val roomName = "test_room_name"
    val roomPasswordHash = "12345678"

    testPayload(JoinChatRoomPacket(ecPublicKey, userName, roomName, roomPasswordHash), { byteSink ->
      JoinChatRoomPacket.fromByteSink(byteSink)
    }, { restorePacket ->
      assertArrayEquals(ecPublicKey, restorePacket.ecPublicKey)
      assertEquals(userName, restorePacket.userName)
      assertEquals(roomName, restorePacket.roomName)
      assertEquals(roomPasswordHash, restorePacket.roomPasswordHash)
    })
  }

  @Test
  fun testPacketRoomPasswordNull() {
    val ecPublicKey = ByteArray(555) { 0x66.toByte() }
    val userName = "test_user_name"
    val roomName = "test_room_name"
    val roomPasswordHash = null

    testPayload(JoinChatRoomPacket(ecPublicKey, userName, roomName, roomPasswordHash), { byteSink ->
      JoinChatRoomPacket.fromByteSink(byteSink)
    }, { restorePacket ->
      assertArrayEquals(ecPublicKey, restorePacket.ecPublicKey)
      assertEquals(userName, restorePacket.userName)
      assertEquals(roomName, restorePacket.roomName)
      assertEquals(roomPasswordHash, restorePacket.roomPasswordHash)
    })
  }
}