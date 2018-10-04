package core.packet

import core.Packet
import core.byte_sink.ByteSink
import core.byte_sink.InMemoryByteSink
import kotlin.test.assertEquals

open class BasePacketPayloadTest {

  protected fun <T> testPayload(basePacket: BasePacket, restoreFunction: (ByteSink) -> T, testFunction: (T) -> Unit) {
    val response = basePacket.buildPacket(-1L)

    val bodySize = response.bodySize
    val byteSink = (response.packetBody.bodyByteSink as InMemoryByteSink)

    val calculatedBodySize = byteSink.getWriterPosition()
    val responseBytesHex = byteSink.getArray()

    assertEquals(bodySize - Packet.PACKET_BODY_SIZE, calculatedBodySize)
    assertEquals(calculatedBodySize, responseBytesHex.size)

    val restoredResponse = restoreFunction(byteSink)

    testFunction(restoredResponse)
  }


}