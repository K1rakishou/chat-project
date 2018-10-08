package core.response

import core.ResponseType
import core.Status
import core.byte_sink.ByteSink
import core.exception.*

class SendChatMessageResponsePayload private constructor(
  status: Status
) : BaseResponse(status) {

  override val packetType: Short
    get() = ResponseType.SendChatMessageResponseType.value

  override fun getPayloadSize(): Int {
    return super.getPayloadSize()
  }

  override fun toByteSink(byteSink: ByteSink) {
    byteSink.writeShort(CURRENT_RESPONSE_VERSION.value)

    when (CURRENT_RESPONSE_VERSION) {
      SendChatMessageResponsePayload.ResponseVersion.V1 -> {
        byteSink.writeShort(status.value)
      }
      SendChatMessageResponsePayload.ResponseVersion.Unknown -> throw UnknownPacketVersionException(CURRENT_RESPONSE_VERSION.value)
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

    fun success(): SendChatMessageResponsePayload {
      return SendChatMessageResponsePayload(Status.Ok)
    }

    fun fail(status: Status): SendChatMessageResponsePayload {
      return SendChatMessageResponsePayload(status)
    }

    @Throws(ResponseDeserializationException::class)
    fun fromByteSink(byteSink: ByteSink): SendChatMessageResponsePayload {
      try {
        val responseVersion = ResponseVersion.fromShort(byteSink.readShort())
        when (responseVersion) {
          SendChatMessageResponsePayload.ResponseVersion.V1 -> {
            val status = Status.fromShort(byteSink.readShort())
            if (status != Status.Ok) {
              return fail(status)
            }

            return SendChatMessageResponsePayload(status)
          }
          SendChatMessageResponsePayload.ResponseVersion.Unknown -> throw UnknownPacketVersionException(responseVersion.value)
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