package core.packet

import org.junit.Assert.*
import org.junit.Test

class CreateRoomPacketPayloadTest {

  @Test
  fun testSize() {
    val isPublic = true
    val roomName = "test346346346"
    val roomPassword = "testtestyzdr7dzr7347z4ew"

    assertEquals(68, CreateRoomPacketPayload(isPublic, roomName, roomPassword).packetToBytes(0L).size)
  }

  @Test
  fun testPacketSerializationDeserialization() {
    val isPublic = true
    val roomName = "test346346346"
    val roomPassword = "testtestyzdr7dzr7347z4ew"

    val bytes = CreateRoomPacketPayload(isPublic, roomName, roomPassword).packetToBytes(0L)
    val packet = CreateRoomPacketPayload.fromByteArray(bytes.copyOfRange(18, bytes.size))

    assertEquals(isPublic, packet.isPublic)
    assertEquals(roomName, packet.chatRoomName)
    assertEquals(roomPassword, packet.chatRoomPasswordHash)
  }
}