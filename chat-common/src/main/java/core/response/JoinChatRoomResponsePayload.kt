package core.response

import core.model.drainable.PublicUserInChat
import core.ResponseType
import core.Status
import core.byte_sink.ByteSink
import core.sizeofList

class JoinChatRoomResponsePayload private constructor(
  status: Status,
  private val users: List<PublicUserInChat> = emptyList()
) : BaseResponse(status) {

  override val packetType: Short
    get() = ResponseType.JoinChatRoomResponseType.value

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + sizeofList(users)
  }

  override fun toByteSink(byteSink: ByteSink) {
    byteSink.writeShort(CURRENT_RESPONSE_VERSION.value)

    when (CURRENT_RESPONSE_VERSION) {
      JoinChatRoomResponsePayload.ResponseVersion.V1 -> {
        byteSink.writeShort(status.value)
        byteSink.writeList(users)
      }
    }
  }

  enum class ResponseVersion(val value: Short) {
    V1(1);

    companion object {
      fun fromShort(value: Short): ResponseVersion {
        return ResponseVersion.values().first { it.value == value }
      }
    }
  }

  companion object {
    private val CURRENT_RESPONSE_VERSION = ResponseVersion.V1

    fun success(users: List<PublicUserInChat>): JoinChatRoomResponsePayload {
      return JoinChatRoomResponsePayload(Status.Ok, users)
    }

    fun fail(status: Status): JoinChatRoomResponsePayload {
      return JoinChatRoomResponsePayload(status)
    }
  }
}