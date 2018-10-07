package core.packet

import core.Constants
import core.exception.ByteSinkBufferOverflowException
import core.security.SecurityUtils
import org.junit.Assert.*
import org.junit.Test

class JoinChatRoomPacketV1Test : BasePacketPayloadTest() {

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

  @Test(expected = ByteSinkBufferOverflowException::class)
  fun testPacketExceedUserName() {
    val ecPublicKey = ByteArray(555) { 0x66.toByte() }
    val userName = SecurityUtils.Generator.generateRandomString(Constants.maxUserNameLen + 10)
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

  @Test(expected = ByteSinkBufferOverflowException::class)
  fun testPacketExceedRoomName() {
    val ecPublicKey = ByteArray(555) { 0x66.toByte() }
    val userName = "test_user_name"
    val roomName = SecurityUtils.Generator.generateRandomString(Constants.maxChatRoomNameLength + 10)
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

  @Test(expected = ByteSinkBufferOverflowException::class)
  fun testPacketExceedRoomPassword() {
    val ecPublicKey = ByteArray(555) { 0x66.toByte() }
    val userName = "test_user_name"
    val roomName = "test_room_name"
    val roomPasswordHash = SecurityUtils.Generator.generateRandomString(Constants.maxChatRoomPasswordHash + 10)

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