package core.response

import core.model.drainable.PublicUserInChat
import core.ResponseType
import core.Status
import core.byte_sink.ByteSink
import core.exception.UnknownPacketVersion
import core.sizeof

class UserHasJoinedResponsePayload private constructor(
  status: Status,
  private val user: PublicUserInChat? = null
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
        byteSink.writeDrainable(user)
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

    fun fromByteSink(byteSink: ByteSink): UserHasJoinedResponsePayload {
      val responseVersion = ResponseVersion.fromShort(byteSink.readShort())

      when (responseVersion) {
        UserHasJoinedResponsePayload.ResponseVersion.V1 -> {
          val status = Status.fromShort(byteSink.readShort())

          //TODO: check status code before trying to deserialize the rest of the body
          val user = byteSink.readDrainable<PublicUserInChat>(PublicUserInChat::class)
          return UserHasJoinedResponsePayload(status, user)
        }
        UserHasJoinedResponsePayload.ResponseVersion.Unknown -> throw UnknownPacketVersion()
      }
    }
  }
}