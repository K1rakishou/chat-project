package core.packet

import core.Packet
import core.byte_sink.ByteSink
import core.byte_sink.InMemoryByteSink
import core.byte_sink.OnDiskByteSink
import java.io.File
import kotlin.test.assertEquals

open class BasePacketPayloadTest {
  private val testFilePath = File("D:\\projects\\data\\chat\\test_file")

  protected fun <T> testPayload(basePacket: BasePacket, restoreFunction: (ByteSink) -> T, testFunction: (T) -> Unit) {
    testWithInMemoryByteSink(basePacket, restoreFunction, testFunction)
    testWithOnDiskMemoryByteSink(basePacket, restoreFunction, testFunction)
  }

  private fun <T> testWithInMemoryByteSink(basePacket: BasePacket, restoreFunction: (ByteSink) -> T, testFunction: (T) -> Unit) {
    val response = PacketBuilder.buildPacket(basePacket, InMemoryByteSink.createWithInitialSize(basePacket.getPayloadSize()))
    val bodySize = response.bodySize

    (response.packetBody.bodyByteSink as InMemoryByteSink).use { byteSink ->
      val calculatedBodySize = byteSink.getWriterPosition()
      val responseBytesHex = byteSink.getArray()

      assertEquals(bodySize - Packet.PACKET_BODY_SIZE, calculatedBodySize)
      assertEquals(calculatedBodySize, responseBytesHex.size)

      val restoredResponse = restoreFunction(byteSink)
      testFunction(restoredResponse)
    }
  }

  private fun <T> testWithOnDiskMemoryByteSink(basePacket: BasePacket, restoreFunction: (ByteSink) -> T, testFunction: (T) -> Unit) {
    if (!testFilePath.exists()) {
      testFilePath.createNewFile()
    }

    val response = PacketBuilder.buildPacket(basePacket, OnDiskByteSink.fromFile(testFilePath, 32))
    val bodySize = response.bodySize

    (response.packetBody.bodyByteSink as OnDiskByteSink).use { byteSink ->
      val calculatedBodySize = byteSink.getWriterPosition()
      val responseBytesHex = byteSink.getArray()

      assertEquals(bodySize - Packet.PACKET_BODY_SIZE, calculatedBodySize)
      assertEquals(calculatedBodySize, responseBytesHex.size)

      val restoredResponse = restoreFunction(byteSink)
      testFunction(restoredResponse)
    }
  }
}