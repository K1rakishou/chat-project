package core.response

import core.Constants
import core.Status
import core.exception.ResponseDeserializationException
import core.model.drainable.ChatRoomData
import core.security.SecurityUtils
import org.junit.Test
import kotlin.test.assertEquals

class GetPageOfPublicRoomsResponsePayloadTest : BaseResponsePayloadTest() {

  @Test
  fun testResponse() {
    val status = Status.Ok
    val publicChatRoomList = listOf(
      ChatRoomData("testRoom1", "111", false),
      ChatRoomData("testRoom154364r6dr76rt7", "222", true),
      ChatRoomData("45476d478", "333", false),
      ChatRoomData("88888888888888888888888888888888888888888888888", "444", false),
      ChatRoomData("545745", "555", true)
    )

    testPayload(GetPageOfPublicRoomsResponsePayload.success(publicChatRoomList), { byteSink ->
      GetPageOfPublicRoomsResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(status, restoredResponse.status)

      for (i in 0 until publicChatRoomList.size) {
        assertEquals(restoredResponse.publicChatRoomList[i].chatRoomName, restoredResponse.publicChatRoomList[i].chatRoomName)
        assertEquals(restoredResponse.publicChatRoomList[i].chatRoomImageUrl, restoredResponse.publicChatRoomList[i].chatRoomImageUrl)
        assertEquals(restoredResponse.publicChatRoomList[i].isPublic, restoredResponse.publicChatRoomList[i].isPublic)
      }
    })
  }

  @Test
  fun testResponseRoomsListIsEmpty() {
    val status = Status.Ok
    val publicChatRoomList = listOf<ChatRoomData>()

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
    val publicChatRoomList = listOf<ChatRoomData>()

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
      ChatRoomData("testRoom1", "111", false),
      ChatRoomData("testRoom154364r6dr76rt7", "222", true),
      ChatRoomData("45476d478", "333", false),
      ChatRoomData(SecurityUtils.Generator.generateRandomString(Constants.maxChatRoomNameLen + 10), "444", false),
      ChatRoomData("545745", "555", true)
    )

    testPayload(GetPageOfPublicRoomsResponsePayload.success(publicChatRoomList), { byteSink ->
      GetPageOfPublicRoomsResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(status, restoredResponse.status)

      for (i in 0 until publicChatRoomList.size) {
        assertEquals(restoredResponse.publicChatRoomList[i].chatRoomName, restoredResponse.publicChatRoomList[i].chatRoomName)
        assertEquals(restoredResponse.publicChatRoomList[i].chatRoomImageUrl, restoredResponse.publicChatRoomList[i].chatRoomImageUrl)
        assertEquals(restoredResponse.publicChatRoomList[i].isPublic, restoredResponse.publicChatRoomList[i].isPublic)
      }
    })
  }

  @Test(expected = ResponseDeserializationException::class)
  fun testResponseExceedChatImageUrl() {
    val status = Status.Ok
    val publicChatRoomList = listOf(
      ChatRoomData("testRoom1", "111", false),
      ChatRoomData("testRoom154364r6dr76rt7", "222", true),
      ChatRoomData("45476d478", "333", false),
      ChatRoomData("32423", SecurityUtils.Generator.generateRandomString(Constants.maxChatRoomImageUrlLen + 10), false),
      ChatRoomData("545745", "555", true)
    )

    testPayload(GetPageOfPublicRoomsResponsePayload.success(publicChatRoomList), { byteSink ->
      GetPageOfPublicRoomsResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(status, restoredResponse.status)

      for (i in 0 until publicChatRoomList.size) {
        assertEquals(restoredResponse.publicChatRoomList[i].chatRoomName, restoredResponse.publicChatRoomList[i].chatRoomName)
        assertEquals(restoredResponse.publicChatRoomList[i].chatRoomImageUrl, restoredResponse.publicChatRoomList[i].chatRoomImageUrl)
        assertEquals(restoredResponse.publicChatRoomList[i].isPublic, restoredResponse.publicChatRoomList[i].isPublic)
      }
    })
  }
}