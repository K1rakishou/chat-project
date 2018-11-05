package core.response

import core.Constants
import core.Status
import core.exception.ResponseDeserializationException
import core.model.drainable.ChatRoomData
import core.security.SecurityUtils
import org.junit.Assert.*
import org.junit.Test

class SearchChatRoomResponsePayloadTest : BaseResponsePayloadTest() {

  @Test
  fun `test response success`() {
    val foundChatRooms = listOf(
      ChatRoomData("test1", "awrawrawr", true),
      ChatRoomData("test2", "awrawrawr", true),
      ChatRoomData("test3", "awrawrawr", false)
    )

    testPayload(SearchChatRoomResponsePayload.success(foundChatRooms), { byteSink ->
      SearchChatRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      for (i in 0 until restoredResponse.foundChatRooms.size) {
        assertEquals(foundChatRooms[i].chatRoomName, restoredResponse.foundChatRooms[i].chatRoomName)
        assertEquals(foundChatRooms[i].chatRoomImageUrl, restoredResponse.foundChatRooms[i].chatRoomImageUrl)
        assertEquals(foundChatRooms[i].isPublic, restoredResponse.foundChatRooms[i].isPublic)
      }
    })
  }

  @Test
  fun `test response fail`() {
    testPayload(SearchChatRoomResponsePayload.fail(Status.BadParam), { byteSink ->
      SearchChatRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(Status.BadParam, restoredResponse.status)
    })
  }

  @Test
  fun `test response with empty found chat rooms`() {
    val foundChatRooms = emptyList<ChatRoomData>()

    testPayload(SearchChatRoomResponsePayload.success(foundChatRooms), { byteSink ->
      SearchChatRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      for (i in 0 until restoredResponse.foundChatRooms.size) {
        assertEquals(foundChatRooms[i].chatRoomName, restoredResponse.foundChatRooms[i].chatRoomName)
        assertEquals(foundChatRooms[i].chatRoomImageUrl, restoredResponse.foundChatRooms[i].chatRoomImageUrl)
        assertEquals(foundChatRooms[i].isPublic, restoredResponse.foundChatRooms[i].isPublic)
      }
    })
  }

  @Test(expected = ResponseDeserializationException::class)
  fun `test response with chatRoomName exceeding maxChatRoomNameLen`() {
    val foundChatRooms = listOf(
      ChatRoomData(SecurityUtils.Generator.generateRandomString(Constants.maxChatRoomNameLength + 10), "awrawrawr", true)
    )

    testPayload(SearchChatRoomResponsePayload.success(foundChatRooms), { byteSink ->
      SearchChatRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      for (i in 0 until restoredResponse.foundChatRooms.size) {
        assertEquals(foundChatRooms[i].chatRoomName, restoredResponse.foundChatRooms[i].chatRoomName)
        assertEquals(foundChatRooms[i].chatRoomImageUrl, restoredResponse.foundChatRooms[i].chatRoomImageUrl)
        assertEquals(foundChatRooms[i].isPublic, restoredResponse.foundChatRooms[i].isPublic)
      }
    })
  }

  @Test(expected = ResponseDeserializationException::class)
  fun `test response with chatRoomImageUrl exceeding maxChatRoomNameLen`() {
    val foundChatRooms = listOf(
      ChatRoomData("test1", SecurityUtils.Generator.generateRandomString(Constants.maxImageUrlLen + 10), true)
    )

    testPayload(SearchChatRoomResponsePayload.success(foundChatRooms), { byteSink ->
      SearchChatRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      for (i in 0 until restoredResponse.foundChatRooms.size) {
        assertEquals(foundChatRooms[i].chatRoomName, restoredResponse.foundChatRooms[i].chatRoomName)
        assertEquals(foundChatRooms[i].chatRoomImageUrl, restoredResponse.foundChatRooms[i].chatRoomImageUrl)
        assertEquals(foundChatRooms[i].isPublic, restoredResponse.foundChatRooms[i].isPublic)
      }
    })
  }
}