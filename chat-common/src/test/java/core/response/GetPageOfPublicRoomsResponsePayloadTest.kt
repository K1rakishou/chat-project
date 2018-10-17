package core.response

import core.Constants
import core.Status
import core.exception.ResponseDeserializationException
import core.model.drainable.PublicChatRoom
import core.security.SecurityUtils
import org.junit.Test
import kotlin.test.assertEquals

class GetPageOfPublicRoomsResponsePayloadTest : BaseResponsePayloadTest() {

  @Test
  fun testResponse() {
    val status = Status.Ok
    val publicChatRoomList = listOf(
      PublicChatRoom("testRoom1", "111"),
      PublicChatRoom("testRoom154364r6dr76rt7", "222"),
      PublicChatRoom("45476d478", "333"),
      PublicChatRoom("88888888888888888888888888888888888888888888888", "444"),
      PublicChatRoom("545745", "555")
    )

    testPayload(GetPageOfPublicRoomsResponsePayload.success(publicChatRoomList), { byteSink ->
      GetPageOfPublicRoomsResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(status, restoredResponse.status)

      for (i in 0 until publicChatRoomList.size) {
        assertEquals(restoredResponse.publicChatRoomList[i].chatRoomName, restoredResponse.publicChatRoomList[i].chatRoomName)
        assertEquals(restoredResponse.publicChatRoomList[i].chatRoomImageUrl, restoredResponse.publicChatRoomList[i].chatRoomImageUrl)
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

  @Test(expected = ResponseDeserializationException::class)
  fun testResponseExceedChatRoomName() {
    val status = Status.Ok
    val publicChatRoomList = listOf(
      PublicChatRoom("testRoom1", "111"),
      PublicChatRoom("testRoom154364r6dr76rt7", "222"),
      PublicChatRoom("45476d478", "333"),
      PublicChatRoom(SecurityUtils.Generator.generateRandomString(Constants.maxChatRoomNameLength + 10), "444"),
      PublicChatRoom("545745", "555")
    )

    testPayload(GetPageOfPublicRoomsResponsePayload.success(publicChatRoomList), { byteSink ->
      GetPageOfPublicRoomsResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(status, restoredResponse.status)

      for (i in 0 until publicChatRoomList.size) {
        assertEquals(restoredResponse.publicChatRoomList[i].chatRoomName, restoredResponse.publicChatRoomList[i].chatRoomName)
        assertEquals(restoredResponse.publicChatRoomList[i].chatRoomImageUrl, restoredResponse.publicChatRoomList[i].chatRoomImageUrl)
      }
    })
  }

  @Test(expected = ResponseDeserializationException::class)
  fun testResponseExceedChatImageUrl() {
    val status = Status.Ok
    val publicChatRoomList = listOf(
      PublicChatRoom("testRoom1", "111"),
      PublicChatRoom("testRoom154364r6dr76rt7", "222"),
      PublicChatRoom("45476d478", "333"),
      PublicChatRoom("32423", SecurityUtils.Generator.generateRandomString(Constants.maxImageUrlLen + 10)),
      PublicChatRoom("545745", "555")
    )

    testPayload(GetPageOfPublicRoomsResponsePayload.success(publicChatRoomList), { byteSink ->
      GetPageOfPublicRoomsResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(status, restoredResponse.status)

      for (i in 0 until publicChatRoomList.size) {
        assertEquals(restoredResponse.publicChatRoomList[i].chatRoomName, restoredResponse.publicChatRoomList[i].chatRoomName)
        assertEquals(restoredResponse.publicChatRoomList[i].chatRoomImageUrl, restoredResponse.publicChatRoomList[i].chatRoomImageUrl)
      }
    })
  }
}