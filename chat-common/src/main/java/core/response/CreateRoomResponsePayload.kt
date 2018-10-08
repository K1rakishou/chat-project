package core.response

import core.Constants
import core.ResponseType
import core.Status
import core.byte_sink.ByteSink
import core.exception.*
import core.sizeof

class CreateRoomResponsePayload private constructor(
  status: Status,
  val chatRoomName: String? = null
) : BaseResponse(status) {

  override val packetType: Short
    get() = ResponseType.CreateRoomResponseType.value

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + when (status) {
      Status.Ok -> sizeof(chatRoomName)
      else -> 0
    }
  }

  override fun toByteSink(byteSink: ByteSink) {
    byteSink.writeShort(CURRENT_RESPONSE_VERSION.value)

    when (CURRENT_RESPONSE_VERSION) {
      CreateRoomResponsePayload.ResponseVersion.V1 -> {
        byteSink.writeShort(status.value)

        if (status == Status.Ok) {
          byteSink.writeString(chatRoomName)
        }
      }
      CreateRoomResponsePayload.ResponseVersion.Unknown -> throw UnknownPacketVersionException(CURRENT_RESPONSE_VERSION.value)
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

    fun success(chatRoomName: String): CreateRoomResponsePayload {
      return CreateRoomResponsePayload(Status.Ok, chatRoomName)
    }

    fun fail(status: Status): CreateRoomResponsePayload {
      return CreateRoomResponsePayload(status)
    }

    @Throws(ResponseDeserializationException::class)
    fun fromByteSink(byteSink: ByteSink): CreateRoomResponsePayload {
      try {
        val responseVersion = ResponseVersion.fromShort(byteSink.readShort())
        return when (responseVersion) {
          CreateRoomResponsePayload.ResponseVersion.V1 -> {
            val status = Status.fromShort(byteSink.readShort())
            if (status != Status.Ok) {
              return fail(status)
            }

            val chatRoomName = byteSink.readString(Constants.maxChatRoomNameLength)
              ?: throw ResponseDeserializationException("Could not read chatRoomName")

            CreateRoomResponsePayload(status, chatRoomName)
          }
          CreateRoomResponsePayload.ResponseVersion.Unknown -> throw UnknownPacketVersionException(responseVersion.value)
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