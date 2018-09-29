package core.response

import core.PositionAwareByteArray
import core.Status
import core.sizeof

open class StatusResponse(
  val status: Status
) : BaseResponse() {

  override fun getSize(): Int {
    return sizeof(status.value)
  }

  override fun toByteArray(byteArray: PositionAwareByteArray) {
    byteArray.writeShort(status.value)
  }
}