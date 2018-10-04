package core

import core.byte_sink.ByteSink

class ResponseInfo(
  val responseId: Long,
  val responseType: ResponseType,
  val byteSink: ByteSink
)