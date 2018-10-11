package core.response

import core.*
import core.model.drainable.PublicUserInChat
import core.byte_sink.ByteSink
import core.exception.*
import core.model.drainable.chat_message.BaseChatMessage

class JoinChatRoomResponsePayload private constructor(
  status: Status,
  val roomName: String? = null,
  val messageHistory: List<BaseChatMessage> = emptyList(),
  val users: List<PublicUserInChat> = emptyList()
) : BaseResponse(status) {

  override fun getResponseType(): ResponseType = ResponseType.JoinChatRoomResponseType
  override fun getResponseVersion(): Short = CURRENT_RESPONSE_VERSION.value

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + when (status) {
      Status.Ok -> sizeof(roomName) + sizeofList(messageHistory) + sizeofList(users)
      else -> 0
    }
  }

  override fun toByteSink(byteSink: ByteSink) {
    super.toByteSink(byteSink)

    when (CURRENT_RESPONSE_VERSION) {
      JoinChatRoomResponsePayload.ResponseVersion.V1 -> {
        byteSink.writeShort(status.value)

        if (status == Status.Ok) {
          byteSink.writeString(roomName)
          byteSink.writeList(messageHistory)
          byteSink.writeList(users)
        }
      }
      JoinChatRoomResponsePayload.ResponseVersion.Unknown -> throw UnknownPacketVersionException(CURRENT_RESPONSE_VERSION.value)
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

    fun success(
      roomName: String,
      messageHistory: List<BaseChatMessage>,
      users: List<PublicUserInChat>
    ): JoinChatRoomResponsePayload {
      return JoinChatRoomResponsePayload(Status.Ok, roomName, messageHistory, users)
    }

    fun fail(status: Status): JoinChatRoomResponsePayload {
      return JoinChatRoomResponsePayload(status)
    }

    @Throws(ResponseDeserializationException::class)
    fun fromByteSink(byteSink: ByteSink): JoinChatRoomResponsePayload {
      try {
        val responseVersion = ResponseVersion.fromShort(byteSink.readShort())
        return when (responseVersion) {
          JoinChatRoomResponsePayload.ResponseVersion.V1 -> {
            val status = Status.fromShort(byteSink.readShort())
            if (status != Status.Ok) {
              return fail(status)
            }

            val chatRoomName = byteSink.readString(Constants.maxChatRoomNameLength)
              ?: throw ResponseDeserializationException("Could not read chatRoomName")
            val messageHistory = byteSink.readList<BaseChatMessage>(BaseChatMessage::class, Constants.maxRoomHistoryMessagesCount)
            val users = byteSink.readList<PublicUserInChat>(PublicUserInChat::class, Constants.maxUsersInRoomCount)

            JoinChatRoomResponsePayload(status, chatRoomName, messageHistory, users)
          }
          JoinChatRoomResponsePayload.ResponseVersion.Unknown -> throw UnknownPacketVersionException(responseVersion.value)
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