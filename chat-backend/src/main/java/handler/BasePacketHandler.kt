package handler

abstract class BasePacketHandler {
  abstract suspend fun handle(packetId: Long, packetPayloadRaw: ByteArray, clientAddress: String)
}