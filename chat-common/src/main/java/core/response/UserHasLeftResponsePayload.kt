package core.response

import core.Constants
import core.ResponseType
import core.Status
import core.byte_sink.ByteSink
import core.exception.*
import core.sizeof

class UserHasLeftResponsePayload private constructor(
  status: Status,
  val roomName: String? = null,
  val userName: String? = null
) : BaseResponse(status) {

  override fun getResponseType(): ResponseType = ResponseType.UserHasLeftResponseType
  override fun getResponseVersion(): Short = CURRENT_RESPONSE_VERSION.value

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + when (status) {
      Status.Ok -> sizeof(roomName) + sizeof(userName)
      else -> 0
    }
  }

  override fun toByteSink(byteSink: ByteSink) {
    super.toByteSink(byteSink)

    when (CURRENT_RESPONSE_VERSION) {
      ResponseVersion.V1 -> {
        byteSink.writeShort(status.value)

        if (status == Status.Ok) {
          byteSink.writeString(roomName)
          byteSink.writeString(userName)
        }
      }
      ResponseVersion.Unknown -> throw UnknownPacketVersionException(CURRENT_RESPONSE_VERSION.value )
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

    fun success(roomName: String, userName: String): UserHasLeftResponsePayload {
      return UserHasLeftResponsePayload(Status.Ok, roomName, userName)
    }

    fun fail(status: Status): UserHasLeftResponsePayload {
      return UserHasLeftResponsePayload(status)
    }

    @Throws(ResponseDeserializationException::class)
    fun fromByteSink(byteSink: ByteSink): UserHasLeftResponsePayload {
      try {
        val responseVersion = ResponseVersion.fromShort(byteSink.readShort())
        when (responseVersion) {
          ResponseVersion.V1 -> {
            val status = Status.fromShort(byteSink.readShort())
            if (status != Status.Ok) {
              return fail(status)
            }

            val roomName = byteSink.readString(Constants.maxChatRoomNameLen)
              ?: throw ResponseDeserializationException("Could not read roomName")
            val userName = byteSink.readString(Constants.maxUserNameLen)
              ?: throw ResponseDeserializationException("Could not read userName")

            return UserHasLeftResponsePayload(status, roomName, userName)
          }
          ResponseVersion.Unknown -> throw UnknownPacketVersionException(responseVersion.value)
        }
      } catch (error: Throwable) {
        when (error) {
          is ByteSinkBufferOverflowException,
          is ReaderPositionExceededBufferSizeException,
          is MaxListSizeExceededException,
          is UnknownPacketVersionException,
          is DrainableDeserializationException -> {
            throw ResponseDeserializationException(error.message ?: "No exception message")
          }
          else -> throw error
        }
      }
    }
  }
}