package core.response

import core.model.drainable.PublicUserInChat
import core.ResponseType
import core.Status
import core.byte_sink.ByteSink
import core.exception.ResponseDeserializationException
import core.exception.UnknownPacketVersion
import core.sizeof

class UserHasJoinedResponsePayload private constructor(
  status: Status,
  val roomName: String? = null,
  val user: PublicUserInChat? = null
) : BaseResponse(status) {

  override val packetType: Short
    get() = ResponseType.UserHasJoinedResponseType.value

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + sizeof(roomName) + sizeof(user)
  }

  override fun toByteSink(byteSink: ByteSink) {
    byteSink.writeShort(CURRENT_RESPONSE_VERSION.value)

    when (CURRENT_RESPONSE_VERSION) {
      UserHasJoinedResponsePayload.ResponseVersion.V1 -> {
        byteSink.writeShort(status.value)
        byteSink.writeString(roomName)
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

    fun success(roomName: String, user: PublicUserInChat): UserHasJoinedResponsePayload {
      return UserHasJoinedResponsePayload(Status.Ok, roomName, user)
    }

    fun fail(status: Status): UserHasJoinedResponsePayload {
      return UserHasJoinedResponsePayload(status)
    }

    fun fromByteSink(byteSink: ByteSink): UserHasJoinedResponsePayload {
      val responseVersion = ResponseVersion.fromShort(byteSink.readShort())

      when (responseVersion) {
        UserHasJoinedResponsePayload.ResponseVersion.V1 -> {
          val status = Status.fromShort(byteSink.readShort())
          if (status != Status.Ok) {
            return UserHasJoinedResponsePayload.fail(status)
          }

          val roomName = byteSink.readString()
            ?: throw ResponseDeserializationException("Could not read roomName")
          val user = byteSink.readDrainable<PublicUserInChat>(PublicUserInChat::class)
            ?: throw ResponseDeserializationException("Could not read user")

          return UserHasJoinedResponsePayload(status, roomName, user)
        }
        UserHasJoinedResponsePayload.ResponseVersion.Unknown -> throw UnknownPacketVersion()
      }
    }
  }
}