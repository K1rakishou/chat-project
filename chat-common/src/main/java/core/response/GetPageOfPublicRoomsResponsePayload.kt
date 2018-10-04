package core.response

import core.*
import core.byte_sink.ByteSink
import core.model.drainable.PublicChatRoom

class GetPageOfPublicRoomsResponsePayload(
  status: Status,
  val publicChatRoomList: List<PublicChatRoom>
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
    }
  }

  enum class ResponseVersion(val value: Short) {
    V1(1);

    companion object {
      fun fromShort(value: Short): ResponseVersion {
        return ResponseVersion.values().first { it.value == value }
      }
    }
  }

  companion object {
    private val CURRENT_RESPONSE_VERSION = ResponseVersion.V1

    fun fromByteSink(byteSink: ByteSink): GetPageOfPublicRoomsResponsePayload {
      val responseVersion = ResponseVersion.fromShort(byteSink.readShort())

      when (responseVersion) {
        GetPageOfPublicRoomsResponsePayload.ResponseVersion.V1 -> {
          val status = Status.fromShort(byteSink.readShort())
          val publicChatRoomList = byteSink.readList<PublicChatRoom>(PublicChatRoom::class)

          return GetPageOfPublicRoomsResponsePayload(status, publicChatRoomList)
        }
      }
    }
  }
}