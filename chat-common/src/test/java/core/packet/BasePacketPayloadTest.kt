package core.packet

import core.byte_sink.ByteSink
import core.byte_sink.InMemoryByteSink
import core.byte_sink.OnDiskByteSink
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

open class BasePacketPayloadTest {
  private val packetBuilder = PacketBuilder()
  private val testFilePath = File("D:\\projects\\data\\chat\\test_file")

  protected fun <T> testPayload(basePacket: BasePacket, restoreFunction: (ByteSink) -> T, testFunction: (T) -> Unit) {
    kotlin.run {
      testPacket(basePacket, InMemoryByteSink.createWithInitialSize(basePacket.getPayloadSize()), restoreFunction, testFunction)
    }
    kotlin.run {
      if (!testFilePath.exists()) {
        testFilePath.createNewFile()
      }

      testPacket(basePacket, OnDiskByteSink.fromFile(testFilePath, basePacket.getPayloadSize()), restoreFunction, testFunction)
    }
  }

  private fun <T> testPacket(basePacket: BasePacket, _byteSink: ByteSink, restoreFunction: (ByteSink) -> T, testFunction: (T) -> Unit) {
    val response = packetBuilder.buildPacket(basePacket, _byteSink)
    val bodySize = response.bodySize

    response.bodyByteSink.use { byteSink ->
      val calculatedBodySize = byteSink.getWriterPosition()
      val responseBytesHex = byteSink.getArray()

      assertEquals(bodySize, calculatedBodySize)
      assertEquals(calculatedBodySize, responseBytesHex.size)

      val restoredResponse = restoreFunction(byteSink)
      testFunction(restoredResponse)
    }

    assertTrue(response.bodyByteSink.isClosed())
  }
}