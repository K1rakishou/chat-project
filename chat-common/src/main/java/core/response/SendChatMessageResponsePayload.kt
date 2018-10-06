package core.response

import core.ResponseType
import core.Status
import core.byte_sink.ByteSink
import core.exception.UnknownPacketVersion

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
      SendChatMessageResponsePayload.ResponseVersion.Unknown -> throw UnknownPacketVersion()
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

    fun fromByteSink(byteSink: ByteSink): SendChatMessageResponsePayload {
      val responseVersion = ResponseVersion.fromShort(byteSink.readShort())

      when (responseVersion) {
        SendChatMessageResponsePayload.ResponseVersion.V1 -> {
          val status = Status.fromShort(byteSink.readShort())

          return SendChatMessageResponsePayload(status)
        }
        SendChatMessageResponsePayload.ResponseVersion.Unknown -> throw UnknownPacketVersion()
      }
    }
  }
}