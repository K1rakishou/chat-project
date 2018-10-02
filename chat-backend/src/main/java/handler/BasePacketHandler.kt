package handler

import core.byte_sink.ByteSink

abstract class BasePacketHandler {
  abstract suspend fun handle(packetId: Long, byteSink: ByteSink, clientAddress: String)
}