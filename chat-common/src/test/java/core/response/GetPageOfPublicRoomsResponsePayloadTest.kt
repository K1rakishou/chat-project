package core.response

import core.Status
import core.model.drainable.PublicChatRoom
import org.junit.Test
import kotlin.test.assertEquals

class GetPageOfPublicRoomsResponsePayloadTest : BaseResponsePayloadTest() {

  @Test
  fun testResponse() {
    val status = Status.UnknownError
    val publicChatRoomList = listOf(
      PublicChatRoom("testRoom1", 0),
      PublicChatRoom("testRoom154364r6dr76rt7", 44),
      PublicChatRoom("45476d478", 52),
      PublicChatRoom("88888888888888888888888888888888888888888888888", 0),
      PublicChatRoom("5", 1)
    )

    testPayload(GetPageOfPublicRoomsResponsePayload(status, publicChatRoomList), { byteSink ->
      GetPageOfPublicRoomsResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      for (i in 0 until publicChatRoomList.size) {
        assertEquals(restoredResponse.publicChatRoomList[i].roomName, restoredResponse.publicChatRoomList[i].roomName)
      }
    })
  }

  @Test
  fun testResponseRoomsListIsEmpty() {
    val status = Status.UnknownError
    val publicChatRoomList = listOf<PublicChatRoom>()

    testPayload(GetPageOfPublicRoomsResponsePayload(status, publicChatRoomList), { byteSink ->
      GetPageOfPublicRoomsResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      for (i in 0 until publicChatRoomList.size) {
        assertEquals(restoredResponse.publicChatRoomList[i].roomName, restoredResponse.publicChatRoomList[i].roomName)
      }
    })
  }
}