package core.response

import core.PositionAwareByteArray
import core.Status

abstract class BaseResponse(
  val status: Status
) {
  abstract fun getResponseType(): ResponseType
  abstract fun getSize(): Int
  abstract fun toByteArray(byteArray: PositionAwareByteArray)
}