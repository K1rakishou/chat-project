package core.response

import core.Status
import core.model.drainable.PublicUserInChat
import org.junit.Assert.*
import org.junit.Test

class UserHasJoinedResponsePayloadTest : BaseResponsePayloadTest() {

  @Test
  fun testResponseSuccess() {
    val roomName = "test room"
    val user = PublicUserInChat("test user", ByteArray(266) { 0xF0.toByte() })

    testPayload(UserHasJoinedResponsePayload.success(roomName, user), { byteSink ->
      UserHasJoinedResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(Status.Ok, restoredResponse.status)
      assertEquals(roomName, restoredResponse.roomName)
      assertEquals(user.userName, restoredResponse.user!!.userName)
      assertArrayEquals(user.ecPublicKey, restoredResponse.user!!.ecPublicKey)
    })
  }

  @Test
  fun testResponseFail() {
    val status = Status.UnknownError

    testPayload(UserHasJoinedResponsePayload.fail(status), { byteSink ->
      UserHasJoinedResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(status, restoredResponse.status)
      assertEquals(null, restoredResponse.roomName)
      assertEquals(null, restoredResponse.user)
    })
  }
}