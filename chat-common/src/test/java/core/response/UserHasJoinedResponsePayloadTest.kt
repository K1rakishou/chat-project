package core.response

import core.Constants
import core.Status
import core.exception.ResponseDeserializationException
import core.model.drainable.PublicUserInChat
import core.security.SecurityUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
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

  @Test(expected = ResponseDeserializationException::class)
  fun testResponseExceedUserNameSize() {
    val roomName = "test room"
    val userName = SecurityUtils.Generator.generateRandomString(Constants.maxUserNameLen + 10)

    val user = PublicUserInChat(userName, ByteArray(266) { 0xF0.toByte() })

    testPayload(UserHasJoinedResponsePayload.success(roomName, user), { byteSink ->
      UserHasJoinedResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(Status.Ok, restoredResponse.status)
      assertEquals(roomName, restoredResponse.roomName)
      assertEquals(user.userName, restoredResponse.user!!.userName)
      assertArrayEquals(user.ecPublicKey, restoredResponse.user!!.ecPublicKey)
    })
  }

  @Test(expected = ResponseDeserializationException::class)
  fun testResponseExceedEcPublicKeySize() {
    val roomName = "test room"
    val ecPublicKey = SecurityUtils.Generator.generateRandomString(Constants.maxEcPublicKeySize + 10).toByteArray()

    val user = PublicUserInChat("test user", ecPublicKey)

    testPayload(UserHasJoinedResponsePayload.success(roomName, user), { byteSink ->
      UserHasJoinedResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(Status.Ok, restoredResponse.status)
      assertEquals(roomName, restoredResponse.roomName)
      assertEquals(user.userName, restoredResponse.user!!.userName)
      assertArrayEquals(user.ecPublicKey, restoredResponse.user!!.ecPublicKey)
    })
  }
}