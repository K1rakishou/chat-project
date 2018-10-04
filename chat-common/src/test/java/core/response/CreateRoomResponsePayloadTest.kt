package core.response

import core.Status
import org.junit.Assert.*
import org.junit.Test

class CreateRoomResponsePayloadTest : BaseResponsePayloadTest() {

  @Test
  fun testResponse() {
    val status = Status.UnknownError
    val chatRoomName = "34235235"

    testPayload(CreateRoomResponsePayload(status, chatRoomName), { byteSink ->
      CreateRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(chatRoomName, restoredResponse.chatRoomName)
    })
  }

  @Test
  fun testResponseEmptyRoomName() {
    val status = Status.UnknownError
    val chatRoomName = ""

    testPayload(CreateRoomResponsePayload(status, chatRoomName), { byteSink ->
      CreateRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(chatRoomName, restoredResponse.chatRoomName)
    })
  }

  @Test
  fun testResponseEmptyRoomNameIsNull() {
    val status = Status.UnknownError
    val chatRoomName = null

    testPayload(CreateRoomResponsePayload(status, chatRoomName), { byteSink ->
      CreateRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(chatRoomName, restoredResponse.chatRoomName)
    })
  }
}