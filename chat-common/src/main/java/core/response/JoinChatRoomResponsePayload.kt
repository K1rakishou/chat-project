package core.response

import core.model.drainable.PublicUserInChat
import core.ResponseType
import core.Status
import core.byte_sink.ByteSink
import core.exception.UnknownPacketVersion
import core.model.drainable.chat_message.BaseChatMessage
import core.sizeof
import core.sizeofList

class JoinChatRoomResponsePayload private constructor(
  status: Status,
  val roomName: String? = null,
  val messageHistory: List<BaseChatMessage> = emptyList(),
  val users: List<PublicUserInChat> = emptyList()
) : BaseResponse(status) {

  override val packetType: Short
    get() = ResponseType.JoinChatRoomResponseType.value

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + sizeof(roomName) + sizeofList(messageHistory) + sizeofList(users)
  }

  override fun toByteSink(byteSink: ByteSink) {
    byteSink.writeShort(CURRENT_RESPONSE_VERSION.value)

    when (CURRENT_RESPONSE_VERSION) {
      JoinChatRoomResponsePayload.ResponseVersion.V1 -> {
        byteSink.writeShort(status.value)
        byteSink.writeString(roomName)
        byteSink.writeList(messageHistory)
        byteSink.writeList(users)
      }
      JoinChatRoomResponsePayload.ResponseVersion.Unknown -> throw UnknownPacketVersion()
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

    fun fromByteSink(byteSink: ByteSink): JoinChatRoomResponsePayload {
      val responseVersion = ResponseVersion.fromShort(byteSink.readShort())

      return when (responseVersion) {
        JoinChatRoomResponsePayload.ResponseVersion.V1 -> {
          val status = Status.fromShort(byteSink.readShort())

          //TODO: check status code before trying to deserialize the rest of the body
          val roomName = byteSink.readString()
          val messageHistory = byteSink.readList<BaseChatMessage>(BaseChatMessage::class)
          val users = byteSink.readList<PublicUserInChat>(PublicUserInChat::class)
          JoinChatRoomResponsePayload(status, roomName, messageHistory, users)
        }
        JoinChatRoomResponsePayload.ResponseVersion.Unknown -> throw UnknownPacketVersion()
      }
    }
  }
}