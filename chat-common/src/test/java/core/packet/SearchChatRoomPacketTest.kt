package core.packet

import core.Constants
import core.exception.PacketDeserializationException
import core.security.SecurityUtils
import org.junit.Assert.*
import org.junit.Test

class SearchChatRoomPacketTest : BasePacketPayloadTest() {

  @Test
  fun `test packet`() {
    val chatRoomName = "34234"

    testPayload(SearchChatRoomPacket(chatRoomName), { byteSink ->
      SearchChatRoomPacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(chatRoomName, restoredPacket.chatRoomName)
    })
  }

  @Test
  fun `test packet with empty chatRoomName`() {
    val chatRoomName = ""

    testPayload(SearchChatRoomPacket(chatRoomName), { byteSink ->
      SearchChatRoomPacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(chatRoomName, restoredPacket.chatRoomName)
    })
  }

  @Test(expected = PacketDeserializationException::class)
  fun `test packet with chatRoomName exceeding maxChatRoomNameLen`() {
    val chatRoomName = SecurityUtils.Generator.generateRandomString(Constants.maxChatRoomNameLen + 10)

    testPayload(SearchChatRoomPacket(chatRoomName), { byteSink ->
      SearchChatRoomPacket.fromByteSink(byteSink)
    }, { restoredPacket ->
      assertEquals(chatRoomName, restoredPacket.chatRoomName)
    })
  }
}