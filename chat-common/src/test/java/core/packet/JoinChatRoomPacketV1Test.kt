package core.packet

import core.Constants
import core.exception.PacketDeserializationException
import core.security.SecurityUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class JoinChatRoomPacketV1Test : BasePacketPayloadTest() {

  @Test
  fun testPacket() {
    val rootPublicKey = ByteArray(555) { 0x66.toByte() }
    val sessionPublicKey = ByteArray(777) { 0x55.toByte() }
    val userName = "test_user_name"
    val roomName = "test_room_name"
    val roomPasswordHash = "12345678"

    testPayload(JoinChatRoomPacket(rootPublicKey, sessionPublicKey, userName, roomName, roomPasswordHash), { byteSink ->
      JoinChatRoomPacket.fromByteSink(byteSink)
    }, { restorePacket ->
      assertArrayEquals(rootPublicKey, restorePacket.rootPublicKey)
      assertArrayEquals(sessionPublicKey, restorePacket.sessionPublicKey)
      assertEquals(userName, restorePacket.userName)
      assertEquals(roomName, restorePacket.roomName)
      assertEquals(roomPasswordHash, restorePacket.roomPasswordHash)
    })
  }

  @Test
  fun testPacketRoomPasswordNull() {
    val rootPublicKey = ByteArray(555) { 0x66.toByte() }
    val sessionPublicKey = ByteArray(777) { 0x55.toByte() }
    val userName = "test_user_name"
    val roomName = "test_room_name"
    val roomPasswordHash = null

    testPayload(JoinChatRoomPacket(rootPublicKey, sessionPublicKey, userName, roomName, roomPasswordHash), { byteSink ->
      JoinChatRoomPacket.fromByteSink(byteSink)
    }, { restorePacket ->
      assertArrayEquals(rootPublicKey, restorePacket.rootPublicKey)
      assertArrayEquals(sessionPublicKey, restorePacket.sessionPublicKey)
      assertEquals(userName, restorePacket.userName)
      assertEquals(roomName, restorePacket.roomName)
      assertEquals(roomPasswordHash, restorePacket.roomPasswordHash)
    })
  }

  @Test(expected = PacketDeserializationException::class)
  fun testPacketExceedUserName() {
    val rootPublicKey = ByteArray(555) { 0x66.toByte() }
    val sessionPublicKey = ByteArray(777) { 0x55.toByte() }
    val userName = SecurityUtils.Generator.generateRandomString(Constants.maxUserNameLen + 10)
    val roomName = "test_room_name"
    val roomPasswordHash = "12345678"

    testPayload(JoinChatRoomPacket(rootPublicKey, sessionPublicKey, userName, roomName, roomPasswordHash), { byteSink ->
      JoinChatRoomPacket.fromByteSink(byteSink)
    }, { restorePacket ->
      assertArrayEquals(rootPublicKey, restorePacket.rootPublicKey)
      assertArrayEquals(sessionPublicKey, restorePacket.sessionPublicKey)
      assertEquals(userName, restorePacket.userName)
      assertEquals(roomName, restorePacket.roomName)
      assertEquals(roomPasswordHash, restorePacket.roomPasswordHash)
    })
  }

  @Test(expected = PacketDeserializationException::class)
  fun testPacketExceedRoomName() {
    val rootPublicKey = ByteArray(555) { 0x66.toByte() }
    val sessionPublicKey = ByteArray(777) { 0x55.toByte() }
    val userName = "test_user_name"
    val roomName = SecurityUtils.Generator.generateRandomString(Constants.maxChatRoomNameLength + 10)
    val roomPasswordHash = "12345678"

    testPayload(JoinChatRoomPacket(rootPublicKey, sessionPublicKey, userName, roomName, roomPasswordHash), { byteSink ->
      JoinChatRoomPacket.fromByteSink(byteSink)
    }, { restorePacket ->
      assertArrayEquals(rootPublicKey, restorePacket.rootPublicKey)
      assertArrayEquals(sessionPublicKey, restorePacket.sessionPublicKey)
      assertEquals(userName, restorePacket.userName)
      assertEquals(roomName, restorePacket.roomName)
      assertEquals(roomPasswordHash, restorePacket.roomPasswordHash)
    })
  }

  @Test(expected = PacketDeserializationException::class)
  fun testPacketExceedRoomPassword() {
    val rootPublicKey = ByteArray(555) { 0x66.toByte() }
    val sessionPublicKey = ByteArray(777) { 0x55.toByte() }
    val userName = "test_user_name"
    val roomName = "test_room_name"
    val roomPasswordHash = SecurityUtils.Generator.generateRandomString(Constants.maxChatRoomPasswordHash + 10)

    testPayload(JoinChatRoomPacket(rootPublicKey, sessionPublicKey, userName, roomName, roomPasswordHash), { byteSink ->
      JoinChatRoomPacket.fromByteSink(byteSink)
    }, { restorePacket ->
      assertArrayEquals(rootPublicKey, restorePacket.rootPublicKey)
      assertArrayEquals(sessionPublicKey, restorePacket.sessionPublicKey)
      assertEquals(userName, restorePacket.userName)
      assertEquals(roomName, restorePacket.roomName)
      assertEquals(roomPasswordHash, restorePacket.roomPasswordHash)
    })
  }
}