package core.response

import core.PositionAwareByteArray
import core.Status
import core.sizeof

class CreateRoomResponsePayload(
  status: Status,
  val chatRoomName: String?
) : BaseResponse(status) {

  override fun getResponseType(): ResponseType = ResponseType.CreateRoomResponse

  override fun getSize(): Int {
    return sizeof(status.value) + sizeof(chatRoomName)
  }

  override fun toByteArray(byteArray: PositionAwareByteArray) {
    byteArray.writeShort(CURRENT_RESPONSE_VERSION.value)

    when (CURRENT_RESPONSE_VERSION) {
      CreateRoomResponsePayload.ResponseVersion.V1 -> {
        byteArray.writeShort(status.value)
        byteArray.writeString(chatRoomName)
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

    fun fromByteArray(array: ByteArray): CreateRoomResponsePayload {
      val byteArray = PositionAwareByteArray.fromArray(array)
      val responseVersion = ResponseVersion.fromShort(byteArray.readShort())

      return when (responseVersion) {
        CreateRoomResponsePayload.ResponseVersion.V1 -> {
          val status = Status.fromShort(byteArray.readShort())
          val chatRoomName = byteArray.readString()

          CreateRoomResponsePayload(status, chatRoomName)
        }
      }
    }
  }
}