package core.response

import core.*
import core.byte_sink.ByteSink
import core.byte_sink.InMemoryByteSink

class GetPageOfPublicRoomsResponsePayload(
  status: Status,
  val publicChatRoomList: List<PublicChatRoom>
) : BaseResponse(status) {

  override val packetVersion: Short
    get() = ResponseType.GetPageOfPublicRoomsResponseType.value

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + sizeof(status) + sizeofList(publicChatRoomList)
  }

  override fun toByteSink(byteSink: ByteSink) {
    byteSink.writeShort(CURRENT_RESPONSE_VERSION.value)

    when (CURRENT_RESPONSE_VERSION) {
      GetPageOfPublicRoomsResponsePayload.ResponseVersion.V1 -> {
        byteSink.writeShort(status.value)
        byteSink.writeShort(publicChatRoomList.size)

        publicChatRoomList.forEach { it.toByteSink(byteSink) }
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

    fun fromByteArray(array: ByteArray): GetPageOfPublicRoomsResponsePayload {
      val byteSink = InMemoryByteSink.fromArray(array)
      val responseVersion = ResponseVersion.fromShort(byteSink.readShort())

      when (responseVersion) {
        GetPageOfPublicRoomsResponsePayload.ResponseVersion.V1 -> {
          val status = Status.fromShort(byteSink.readShort())
          val publicChatRoomsCount = byteSink.readShort().toInt()

          val publicChatRoomList = ArrayList<PublicChatRoom>(publicChatRoomsCount)

          for (i in 0 until publicChatRoomsCount) {
            publicChatRoomList += PublicChatRoom.fromByteSink(byteSink)
          }

          return GetPageOfPublicRoomsResponsePayload(status, publicChatRoomList)
        }
      }
    }
  }
}