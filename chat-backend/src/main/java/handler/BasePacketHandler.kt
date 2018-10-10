package handler

import core.byte_sink.ByteSink

abstract class BasePacketHandler {
  abstract suspend fun handle(byteSink: ByteSink, clientAddress: String)
}