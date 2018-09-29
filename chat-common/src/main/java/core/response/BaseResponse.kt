package core.response

import core.PositionAwareByteArray

abstract class BaseResponse {
  abstract fun getSize(): Int
  abstract fun toByteArray(byteArray: PositionAwareByteArray)
}