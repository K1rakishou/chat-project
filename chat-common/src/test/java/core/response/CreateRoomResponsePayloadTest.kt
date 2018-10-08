package core.response

import core.Constants
import core.Status
import core.exception.ResponseDeserializationException
import core.security.SecurityUtils
import org.junit.Assert.*
import org.junit.Test

class CreateRoomResponsePayloadTest : BaseResponsePayloadTest() {

  @Test
  fun testResponse() {
    val status = Status.Ok
    val chatRoomName = "34235235"

    testPayload(CreateRoomResponsePayload.success(chatRoomName), { byteSink ->
      CreateRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(status, restoredResponse.status)
      assertEquals(chatRoomName, restoredResponse.chatRoomName)
    })
  }

  @Test
  fun testResponseEmptyRoomName() {
    val status = Status.Ok
    val chatRoomName = ""

    testPayload(CreateRoomResponsePayload.success(chatRoomName), { byteSink ->
      CreateRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(status, restoredResponse.status)
      assertEquals(chatRoomName, restoredResponse.chatRoomName)
    })
  }

  @Test
  fun testResponseFail() {
    val status = Status.UnknownError

    testPayload(CreateRoomResponsePayload.fail(status), { byteSink ->
      CreateRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(status, restoredResponse.status)
      assertEquals(null, restoredResponse.chatRoomName)
    })
  }

  @Test(expected = ResponseDeserializationException::class)
  fun testResponseExceedRoomName() {
    val status = Status.Ok
    val chatRoomName = SecurityUtils.Generator.generateRandomString(Constants.maxChatRoomNameLength + 10)

    testPayload(CreateRoomResponsePayload.success(chatRoomName), { byteSink ->
      CreateRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(status, restoredResponse.status)
      assertEquals(chatRoomName, restoredResponse.chatRoomName)
    })
  }
}