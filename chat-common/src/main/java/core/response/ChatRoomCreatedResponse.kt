package core.response

import core.PositionAwareByteArray
import core.Status
import core.sizeof

class ChatRoomCreatedResponse(
  status: Status,
  val chatRoom: String?
) : StatusResponse(status) {

  override fun getSize(): Int {
    return super.getSize() + sizeof(chatRoom)
  }

  override fun toByteArray(byteArray: PositionAwareByteArray) {
    super.toByteArray(byteArray)
    byteArray.writeString(chatRoom)
  }
}