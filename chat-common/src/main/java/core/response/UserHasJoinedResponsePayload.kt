package core.response

import core.model.drainable.PublicUserInChat
import core.ResponseType
import core.Status
import core.byte_sink.ByteSink
import core.exception.UnknownPacketVersion
import core.sizeof

class UserHasJoinedResponsePayload private constructor(
  status: Status,
  private val user: PublicUserInChat
) : BaseResponse(status) {

  override val packetType: Short
    get() = ResponseType.UserHasJoinedResponseType.value

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + sizeof(status) + user.getSize()
  }

  override fun toByteSink(byteSink: ByteSink) {
    byteSink.writeShort(CURRENT_RESPONSE_VERSION.value)

    when (CURRENT_RESPONSE_VERSION) {
      UserHasJoinedResponsePayload.ResponseVersion.V1 -> {
        byteSink.writeShort(status.value)
        user.serialize(byteSink)
      }
      UserHasJoinedResponsePayload.ResponseVersion.Unknown -> throw UnknownPacketVersion()
    }
  }

  enum class ResponseVersion(val value: Short) {
    Unknown(-1),
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