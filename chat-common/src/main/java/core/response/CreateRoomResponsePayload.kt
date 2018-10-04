package core.response

import core.ResponseType
import core.byte_sink.InMemoryByteSink
import core.Status
import core.byte_sink.ByteSink
import core.sizeof

class CreateRoomResponsePayload(
  status: Status,
  val chatRoomName: String?
) : BaseResponse(status) {

  override val packetType: Short
    get() = ResponseType.CreateRoomResponseType.value

  override fun getPayloadSize(): Int {
    return super.getPayloadSize() + sizeof(chatRoomName)
  }

  override fun toByteSink(byteSink: ByteSink) {
    byteSink.writeShort(CURRENT_RESPONSE_VERSION.value)

    when (CURRENT_RESPONSE_VERSION) {
      CreateRoomResponsePayload.ResponseVersion.V1 -> {
        byteSink.writeShort(status.value)
        byteSink.writeString(chatRoomName)
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

    fun fromByteSink(byteSink: ByteSink): CreateRoomResponsePayload {
      val responseVersion = ResponseVersion.fromShort(byteSink.readShort())

      return when (responseVersion) {
        CreateRoomResponsePayload.ResponseVersion.V1 -> {
          val status = Status.fromShort(byteSink.readShort())
          val chatRoomName = byteSink.readString()

          CreateRoomResponsePayload(status, chatRoomName)
        }
      }
    }
  }
}