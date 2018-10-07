package core.response

import core.*
import core.model.drainable.PublicUserInChat
import core.byte_sink.ByteSink
import core.exception.ResponseDeserializationException
import core.exception.UnknownPacketVersionException

class UserHasJoinedResponsePayload private constructor(
  status: Status,
  val roomName: String? = null,
  val user: PublicUserInChat? = null
) : BaseResponse(status) {

  override val packetType: Short
    get() = ResponseType.UserHasJoinedResponseType.value

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + when (status) {
      Status.Ok -> sizeof(roomName) + sizeof(user)
      else -> 0
    }
  }

  override fun toByteSink(byteSink: ByteSink) {
    byteSink.writeShort(CURRENT_RESPONSE_VERSION.value)

    when (CURRENT_RESPONSE_VERSION) {
      UserHasJoinedResponsePayload.ResponseVersion.V1 -> {
        byteSink.writeShort(status.value)

        if (status == Status.Ok) {
          byteSink.writeString(roomName)
          byteSink.writeDrainable(user)
        }
      }
      UserHasJoinedResponsePayload.ResponseVersion.Unknown -> throw UnknownPacketVersionException(CURRENT_RESPONSE_VERSION.value )
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
            return fail(status)
          }

          val chatRoomName = byteSink.readString(Constants.maxChatRoomNameLength)
            ?: throw ResponseDeserializationException("Could not read chatRoomName")
          val user = byteSink.readDrainable<PublicUserInChat>(PublicUserInChat::class)
            ?: throw ResponseDeserializationException("Could not read user")

          return UserHasJoinedResponsePayload(status, chatRoomName, user)
        }
        UserHasJoinedResponsePayload.ResponseVersion.Unknown -> throw UnknownPacketVersionException(responseVersion.value)
      }
    }
  }
}