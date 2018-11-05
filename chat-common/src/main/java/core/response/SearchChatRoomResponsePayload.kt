package core.response

import core.Constants
import core.ResponseType
import core.Status
import core.byte_sink.ByteSink
import core.exception.*
import core.model.drainable.ChatRoomData
import core.sizeofList

class SearchChatRoomResponsePayload private constructor(
  status: Status,
  val foundChatRooms: List<ChatRoomData>
) : BaseResponse(status) {

  override fun getResponseType(): ResponseType = ResponseType.SearchChatRoomResponseType
  override fun getResponseVersion(): Short = CURRENT_RESPONSE_VERSION.value

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + when(status) {
      Status.Ok -> sizeofList(foundChatRooms)
      else -> 0
    }
  }

  override fun toByteSink(byteSink: ByteSink) {
    super.toByteSink(byteSink)

    when (CURRENT_RESPONSE_VERSION) {
      ResponseVersion.V1 -> {
        byteSink.writeShort(status.value)

        if (status == Status.Ok) {
          byteSink.writeList(foundChatRooms)
        }
      }
      ResponseVersion.Unknown -> throw UnknownPacketVersionException(CURRENT_RESPONSE_VERSION.value)
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

    fun success(foundChatRooms: List<ChatRoomData>): SearchChatRoomResponsePayload {
      return SearchChatRoomResponsePayload(Status.Ok, foundChatRooms)
    }

    fun fail(status: Status): SearchChatRoomResponsePayload {
      return SearchChatRoomResponsePayload(status, emptyList())
    }

    @Throws(ResponseDeserializationException::class)
    fun fromByteSink(byteSink: ByteSink): SearchChatRoomResponsePayload {
      try {
        val responseVersion = ResponseVersion.fromShort(byteSink.readShort())
        when (responseVersion) {
          ResponseVersion.V1 -> {
            val status = Status.fromShort(byteSink.readShort())
            if (status != Status.Ok) {
              return fail(status)
            }

            val foundChatRooms = byteSink.readList<ChatRoomData>(ChatRoomData::class, Constants.maxFoundChatRooms)

            return success(foundChatRooms)
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