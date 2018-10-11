package core.response

import core.Packet
import core.byte_sink.ByteSink

class ResponseBuilder {

  fun buildResponse(response: BaseResponse, byteSink: ByteSink): Packet {
    val payloadSize = response.getPayloadSize()
    if (payloadSize > Int.MAX_VALUE) {
      throw RuntimeException("payloadSize exceeds Int.MAX_VALUE: $payloadSize")
    }

    response.toByteSink(byteSink)

    if (payloadSize != byteSink.getWriterPosition()) {
      throw RuntimeException("payloadSize ($payloadSize) != byteSink.getWriterPosition() (${byteSink.getWriterPosition()})")
    }

    return Packet(
      Packet.MAGIC_NUMBER,
      payloadSize,
      response.getResponseType().value,
      byteSink
    )
  }

}