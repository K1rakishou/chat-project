package core.response

import core.Status
import core.model.drainable.PublicChatRoom
import org.junit.Test
import kotlin.test.assertEquals

class GetPageOfPublicRoomsResponsePayloadTest : BaseResponsePayloadTest() {

  @Test
  fun testResponse() {
    val status = Status.Ok
    val publicChatRoomList = listOf(
      PublicChatRoom("testRoom1", 0),
      PublicChatRoom("testRoom154364r6dr76rt7", 44),
      PublicChatRoom("45476d478", 52),
      PublicChatRoom("88888888888888888888888888888888888888888888888", 0),
      PublicChatRoom("5", 1)
    )

    testPayload(GetPageOfPublicRoomsResponsePayload.success(publicChatRoomList), { byteSink ->
      GetPageOfPublicRoomsResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(status, restoredResponse.status)

      for (i in 0 until publicChatRoomList.size) {
        assertEquals(restoredResponse.publicChatRoomList[i].chatRoomName, restoredResponse.publicChatRoomList[i].chatRoomName)
      }
    })
  }

  @Test
  fun testResponseRoomsListIsEmpty() {
    val status = Status.Ok
    val publicChatRoomList = listOf<PublicChatRoom>()

    testPayload(GetPageOfPublicRoomsResponsePayload.success(publicChatRoomList), { byteSink ->
      GetPageOfPublicRoomsResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(status, restoredResponse.status)

      for (i in 0 until publicChatRoomList.size) {
        assertEquals(restoredResponse.publicChatRoomList[i].chatRoomName, restoredResponse.publicChatRoomList[i].chatRoomName)
      }
    })
  }

  @Test
  fun testResponseFail() {
    val status = Status.UnknownError
    val publicChatRoomList = listOf<PublicChatRoom>()

    testPayload(GetPageOfPublicRoomsResponsePayload.fail(status), { byteSink ->
      GetPageOfPublicRoomsResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(status, restoredResponse.status)

      for (i in 0 until publicChatRoomList.size) {
        assertEquals(restoredResponse.publicChatRoomList[i].chatRoomName, restoredResponse.publicChatRoomList[i].chatRoomName)
      }
    })
  }
}