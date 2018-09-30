package core.response

import core.*

class GetPageOfPublicRoomsResponsePayload(
  status: Status,
  val publicChatRoomList: List<PublicChatRoom>
) : BaseResponse(status) {

  override val packetVersion: Short
    get() = ResponseType.GetPageOfPublicRoomsResponseType.value

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + sizeof(status) + sizeofList(publicChatRoomList)
  }

  override fun toByteArray(byteArray: PositionAwareByteArray) {
    byteArray.writeShort(CURRENT_RESPONSE_VERSION.value)

    when (CURRENT_RESPONSE_VERSION) {
      GetPageOfPublicRoomsResponsePayload.ResponseVersion.V1 -> {
        byteArray.writeShort(status.value)
        byteArray.writeShort(publicChatRoomList.size)

        publicChatRoomList.forEach { it.toByteArray(byteArray) }
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
      val byteArray = PositionAwareByteArray.fromArray(array)
      val responseVersion = ResponseVersion.fromShort(byteArray.readShort())

      when (responseVersion) {
        GetPageOfPublicRoomsResponsePayload.ResponseVersion.V1 -> {
          val status = Status.fromShort(byteArray.readShort())
          val publicChatRoomsCount = byteArray.readShort().toInt()

          val publicChatRoomList = ArrayList<PublicChatRoom>(publicChatRoomsCount)

          for (i in 0 until publicChatRoomsCount) {
            publicChatRoomList += PublicChatRoom.fromByteArray(byteArray)
          }

          return GetPageOfPublicRoomsResponsePayload(status, publicChatRoomList)
        }
      }
    }
  }
}