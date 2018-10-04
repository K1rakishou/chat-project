package core.response

import core.model.drainable.PublicUserInChat
import org.junit.Assert.*
import org.junit.Test

class JoinChatRoomResponsePayloadTest : BaseResponsePayloadTest() {

  @Test
  fun testResponseSuccess() {
    val usersInChatRoom = listOf(
      PublicUserInChat("test1", ByteArray(1) { 0xAA.toByte() })
    )

    testPayload(JoinChatRoomResponsePayload.success(usersInChatRoom), { byteSink ->
      JoinChatRoomResponsePayload.fromByteSink(byteSink)
    }, { restoredResponse ->
      for (i in 0 until usersInChatRoom.size) {
        assertEquals(usersInChatRoom[i].userName, restoredResponse.users[i].userName)
        assertArrayEquals(usersInChatRoom[i].ecPublicKey, restoredResponse.users[i].ecPublicKey)
      }
    })
  }
}