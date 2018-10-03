package core.response

import core.PublicUserInChat
import core.ResponseType
import core.Status
import core.byte_sink.ByteSink
import core.sizeof

class UserHasJoinedResponsePayload private constructor(
  status: Status,
  private val user: PublicUserInChat
) : BaseResponse(status) {

  override val packetType: Short
    get() = ResponseType.UserHasJoinedResponseType.value

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + sizeof(status) + sizeof(user)
  }

  override fun toByteSink(byteSink: ByteSink) {
    byteSink.writeShort(CURRENT_RESPONSE_VERSION.value)

    when (CURRENT_RESPONSE_VERSION) {
      UserHasJoinedResponsePayload.ResponseVersion.V1 -> {
        byteSink.writeShort(status.value)
        user.serialize(byteSink)
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

    fun success(user: PublicUserInChat): UserHasJoinedResponsePayload {
      return UserHasJoinedResponsePayload(Status.Ok, user)
    }
  }
}