package core.response

import core.Status
import core.model.drainable.PublicUserInChat
import org.junit.Assert.*
import org.junit.Test

class JoinChatRoomResponsePayloadTest : BaseResponsePayloadTest() {

  @Test
  fun testResponseSuccess() {
    val usersInChatRoom = listOf(
      PublicUserInChat("test1", ByteArray(277) { 0xAA.toByte() })
    )

    testPayload(JoinChatRoomResponsePayload.success(usersInChatRoom), { byteSink ->
      JoinChatRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(Status.Ok.value, restoredResponse.status.value)

      for (i in 0 until usersInChatRoom.size) {
        assertEquals(usersInChatRoom[i].userName, restoredResponse.users[i].userName)
        assertArrayEquals(usersInChatRoom[i].ecPublicKey, restoredResponse.users[i].ecPublicKey)
      }
    })
  }

  @Test
  fun testResponseFail() {
    val status = Status.UnknownError

    testPayload(JoinChatRoomResponsePayload.fail(status), { byteSink ->
      JoinChatRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(status.value, restoredResponse.status.value)
    })
  }

  @Test
  fun testResponseEmpty() {
    val usersInChatRoom = emptyList<PublicUserInChat>()

    testPayload(JoinChatRoomResponsePayload.success(usersInChatRoom), { byteSink ->
      JoinChatRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      assertEquals(Status.Ok.value, restoredResponse.status.value)

      for (i in 0 until usersInChatRoom.size) {
        assertEquals(usersInChatRoom[i].userName, restoredResponse.users[i].userName)
        assertArrayEquals(usersInChatRoom[i].ecPublicKey, restoredResponse.users[i].ecPublicKey)
      }
    })
  }
}