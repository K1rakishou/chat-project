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

    testPayload(CreateRoomResponsePayload.success(), { byteSink ->
      CreateRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(status, restoredResponse.status)
    })
  }

  @Test
  fun testResponseEmptyRoomName() {
    val status = Status.Ok

    testPayload(CreateRoomResponsePayload.success(), { byteSink ->
      CreateRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(status, restoredResponse.status)
    })
  }

  @Test
  fun testResponseFail() {
    val status = Status.UnknownError

    testPayload(CreateRoomResponsePayload.fail(status), { byteSink ->
      CreateRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(status, restoredResponse.status)
    })
  }
}