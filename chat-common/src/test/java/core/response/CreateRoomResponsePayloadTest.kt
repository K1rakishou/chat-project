package core.response

import core.Status
import org.junit.Assert.*
import org.junit.Test

class CreateRoomResponsePayloadTest {

  @Test
  fun testSize() {
    val status = Status.UnknownError
    val chatRoomName = "test_room_name"

    assertEquals(41, CreateRoomResponsePayload(status, chatRoomName).responseToBytes(1L).size)
  }

  @Test
  fun testSerializationDeserialization() {
    val status = Status.UnknownError
    val chatRoomName = "s5es6se6 w46e5yu z5u z5u 5u"

    val packetBytes = CreateRoomResponsePayload(status, chatRoomName).responseToBytes(1L)
    val packet = CreateRoomResponsePayload.fromByteArray(packetBytes.copyOfRange(18, packetBytes.size))

    assertEquals(status.value, packet.status.value)
    assertEquals(chatRoomName, packet.chatRoomName)
  }
}