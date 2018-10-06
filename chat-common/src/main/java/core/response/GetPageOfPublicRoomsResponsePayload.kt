package core.response

import core.*
import core.byte_sink.ByteSink
import core.exception.UnknownPacketVersion
import core.model.drainable.PublicChatRoom

class GetPageOfPublicRoomsResponsePayload private constructor(
  status: Status,
  val publicChatRoomList: List<PublicChatRoom> = emptyList()
) : BaseResponse(status) {

  override val packetType: Short
    get() = ResponseType.GetPageOfPublicRoomsResponseType.value

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + sizeofList(publicChatRoomList)
  }

  override fun toByteSink(byteSink: ByteSink) {
    byteSink.writeShort(CURRENT_RESPONSE_VERSION.value)

    when (CURRENT_RESPONSE_VERSION) {
      GetPageOfPublicRoomsResponsePayload.ResponseVersion.V1 -> {
        byteSink.writeShort(status.value)
        byteSink.writeList(publicChatRoomList)
      }
      GetPageOfPublicRoomsResponsePayload.ResponseVersion.Unknown -> throw UnknownPacketVersion()
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

    fun success(publicChatRoomList: List<PublicChatRoom>): GetPageOfPublicRoomsResponsePayload {
      return GetPageOfPublicRoomsResponsePayload(Status.Ok, publicChatRoomList)
    }

    fun fail(status: Status): GetPageOfPublicRoomsResponsePayload {
      return GetPageOfPublicRoomsResponsePayload(status)
    }

    fun fromByteSink(byteSink: ByteSink): GetPageOfPublicRoomsResponsePayload {
      val responseVersion = ResponseVersion.fromShort(byteSink.readShort())

      when (responseVersion) {
        GetPageOfPublicRoomsResponsePayload.ResponseVersion.V1 -> {
          val status = Status.fromShort(byteSink.readShort())
          if (status != Status.Ok) {
            return GetPageOfPublicRoomsResponsePayload.fail(status)
          }

          val publicChatRoomList = byteSink.readList<PublicChatRoom>(PublicChatRoom::class, Constants.maxChatRoomsCount)
          return GetPageOfPublicRoomsResponsePayload(status, publicChatRoomList)
        }
        GetPageOfPublicRoomsResponsePayload.ResponseVersion.Unknown -> throw UnknownPacketVersion()
      }
    }
  }
}