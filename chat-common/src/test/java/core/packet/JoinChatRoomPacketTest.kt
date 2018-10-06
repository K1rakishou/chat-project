package core.packet

import core.exception.ByteSinkReadException
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

  @Test(expected = ByteSinkReadException::class)
  fun testPacketExceedUserName() {
    val ecPublicKey = ByteArray(555) { 0x66.toByte() }
    val userName = "test_user_nametest_user_nametest_user_nametest_user_nametest_user_name"
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

  @Test(expected = ByteSinkReadException::class)
  fun testPacketExceedRoomName() {
    val ecPublicKey = ByteArray(555) { 0x66.toByte() }
    val userName = "test_user_name"
    val roomName = "test_room_nametest_room_nametest_room_nametest_room_nametest_room_nametest_room_nametest_room_namete" +
      "st_room_nametest_room_nametest_room_nametest_room_nametest_room_nametest_room_nametest_room_nametest_room_nametest" +
      "_room_nametest_room_nametest_room_nametest_room_nametest_room_name"
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

  @Test(expected = ByteSinkReadException::class)
  fun testPacketExceedRoomPassword() {
    val ecPublicKey = ByteArray(555) { 0x66.toByte() }
    val userName = "test_user_name"
    val roomName = "test_room_name"
    val roomPasswordHash = "12345678123456781234567812345678123456781234567812345678123456781234567812345678123456781234" +
      "56781234567812345678123456781234567812345678123456781234567812345678123456781234567812345678123456781234567812345" +
      "67812345678123456781234567812345678123456781234567812345678123456781234567812345678123456781234567812345678123456" +
      "7812345678123456781234567812345678123456781234567812345678123456781234567812345678"

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