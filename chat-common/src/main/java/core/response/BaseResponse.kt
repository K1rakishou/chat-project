package core.response

import core.ResponseType
import core.Status
import core.byte_sink.ByteSink
import core.sizeof

abstract class BaseResponse(
  val status: Status
) {

  abstract fun getResponseType(): ResponseType
  abstract fun getResponseVersion(): Short

  open fun toByteSink(byteSink: ByteSink) {
    byteSink.writeShort(getResponseVersion())
  }

  open fun getPayloadSize(): Int {
    return sizeof(getResponseVersion()) + sizeof(status)
  }
}