package core.response

import core.Constants
import core.ResponseType
import core.Status
import core.byte_sink.ByteSink
import core.exception.*
import core.sizeof

class NewChatMessageResponsePayload private constructor(
  status: Status,
  val messageId: Int = -1,
  val roomName: String? = null,
  val userName: String? = null,
  val message: String? = null
) : BaseResponse(status) {

  override val packetType: Short
    get() = ResponseType.NewChatMessageResponseType.value

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + sizeof(messageId) + sizeof(roomName) + sizeof(userName) + sizeof(message)
  }

  override fun toByteSink(byteSink: ByteSink) {
    byteSink.writeShort(CURRENT_RESPONSE_VERSION.value)

    when (CURRENT_RESPONSE_VERSION) {
      ResponseVersion.V1 -> {
        byteSink.writeShort(status.value)

        if (status == Status.Ok) {
          byteSink.writeInt(messageId)
          byteSink.writeString(roomName)
          byteSink.writeString(userName)
          byteSink.writeString(message)
        }
      }
      NewChatMessageResponsePayload.ResponseVersion.Unknown -> throw UnknownPacketVersionException(CURRENT_RESPONSE_VERSION.value)
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

    fun success(messageId: Int, roomName: String, userName: String, message: String): NewChatMessageResponsePayload {
      return NewChatMessageResponsePayload(Status.Ok, messageId, roomName, userName, message)
    }

    fun fail(status: Status): NewChatMessageResponsePayload {
      return NewChatMessageResponsePayload(status)
    }

    @Throws(ResponseDeserializationException::class)
    fun fromByteSink(byteSink: ByteSink): NewChatMessageResponsePayload {
      try {
        val responseVersion = ResponseVersion.fromShort(byteSink.readShort())
        when (responseVersion) {
          NewChatMessageResponsePayload.ResponseVersion.V1 -> {
            val status = Status.fromShort(byteSink.readShort())
            if (status != Status.Ok) {
              return fail(status)
            }

            val messageId = byteSink.readInt()
            val roomName = byteSink.readString(Constants.maxChatRoomNameLength)
              ?: throw ResponseDeserializationException("Could not read chatRoomName")
            val userName = byteSink.readString(Constants.maxUserNameLen)
              ?: throw ResponseDeserializationException("Could not read chatRoomName")
            val message = byteSink.readString(Constants.maxTextMessageLen)
              ?: throw ResponseDeserializationException("Could not read chatRoomName")

            return NewChatMessageResponsePayload(status, messageId, roomName, userName, message)
          }
          NewChatMessageResponsePayload.ResponseVersion.Unknown -> throw UnknownPacketVersionException(responseVersion.value)
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