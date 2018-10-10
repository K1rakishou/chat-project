package core

import core.byte_sink.ByteSink

class ResponseInfo(
  val responseType: ResponseType,
  val byteSink: ByteSink
)