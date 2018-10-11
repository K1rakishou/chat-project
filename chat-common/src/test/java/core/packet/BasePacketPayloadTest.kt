package core.packet

import core.Packet
import core.byte_sink.ByteSink
import core.byte_sink.InMemoryByteSink
import core.byte_sink.OnDiskByteSink
import core.security.SecurityUtils
import org.junit.Assert.assertArrayEquals
import java.io.File
import java.lang.IllegalStateException
import java.lang.NullPointerException
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

    assertNotNull(response)
    //TODO
    //can't wait for kotlin's 1.3 contracts to get rid of this
    response!!

    val bodySize = response.bodySize

    response.packetBody.bodyByteSink.use { byteSink ->
      val calculatedBodySize = byteSink.getWriterPosition()
      val responseBytesHex = byteSink.getArray()

      assertEquals(bodySize - Packet.PACKET_BODY_SIZE, calculatedBodySize)
      assertEquals(calculatedBodySize, responseBytesHex.size)

      val restoredResponse = restoreFunction(byteSink)
      testFunction(restoredResponse)
    }

    assertTrue(response.packetBody.bodyByteSink.isClosed())
  }
}