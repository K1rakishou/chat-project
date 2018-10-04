package core.packet

import org.junit.Assert.*
import org.junit.Test

class GetPageOfPublicRoomsPacketTest : BasePacketPayloadTest() {

  @Test
  fun testPacket() {
    val currentPage = 10.toShort()
    val roomsPerPage = 24.toByte()

    testPayload(GetPageOfPublicRoomsPacket(currentPage, roomsPerPage), { byteSink ->
      GetPageOfPublicRoomsPacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(currentPage, restoredPacket.currentPage)
      assertEquals(roomsPerPage, restoredPacket.roomsPerPage)
    })
  }
}