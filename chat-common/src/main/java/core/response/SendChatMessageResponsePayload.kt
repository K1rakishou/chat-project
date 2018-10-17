package core.response

import core.Constants
import core.ResponseType
import core.Status
import core.byte_sink.ByteSink
import core.exception.*
import core.sizeof

class SendChatMessageResponsePayload private constructor(
  status: Status,
  val roomName: String? = null,
  val serverMessageId: Int = -1,
  val clientMessageId: Int = -1
) : BaseResponse(status) {

  override fun getResponseType(): ResponseType = ResponseType.SendChatMessageResponseType
  override fun getResponseVersion(): Short = CURRENT_RESPONSE_VERSION.value

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + when (status) {
      Status.Ok -> sizeof(roomName) + sizeof(serverMessageId) + sizeof(clientMessageId)
      else -> 0
    }
  }

  override fun toByteSink(byteSink: ByteSink) {
    super.toByteSink(byteSink)

    when (CURRENT_RESPONSE_VERSION) {
      SendChatMessageResponsePayload.ResponseVersion.V1 -> {
        byteSink.writeShort(status.value)

        if (status == Status.Ok) {
          byteSink.writeString(roomName)
          byteSink.writeInt(serverMessageId)
          byteSink.writeInt(clientMessageId)
        }
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

    fun success(roomName: String, serverMessageId: Int, clientMessageId: Int): SendChatMessageResponsePayload {
      return SendChatMessageResponsePayload(Status.Ok, roomName, serverMessageId, clientMessageId)
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

            val roomName = byteSink.readString(Constants.maxChatRoomNameLength)
              ?: throw ResponseDeserializationException("Could not read roomName")

            val serverMessageId = byteSink.readInt()
            if (serverMessageId == -1) {
              throw ResponseDeserializationException("Could not read serverMessageId")
            }

            val clientMessageId = byteSink.readInt()
            if (clientMessageId == -1) {
              throw ResponseDeserializationException("Could not read clientMessageId")
            }

            return success(roomName, serverMessageId, clientMessageId)
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