package core.response

import core.Packet
import core.byte_sink.ByteSink
import core.byte_sink.InMemoryByteSink
import core.byte_sink.OnDiskByteSink
import java.io.File
import kotlin.test.assertEquals

open class BaseResponsePayloadTest {
  private val testFilePath = File("D:\\projects\\data\\chat\\test_file")

  protected fun <T> testPayload(baseResponse: BaseResponse, restoreFunction: (ByteSink) -> T, testFunction: (T) -> Unit) {
    testWithInMemoryByteSink(baseResponse, restoreFunction, testFunction)
    testWithOnDiskByteSink(baseResponse, restoreFunction, testFunction)
  }

  private fun <T> testWithInMemoryByteSink(baseResponse: BaseResponse, restoreFunction: (ByteSink) -> T, testFunction: (T) -> Unit) {
    val response = baseResponse.buildResponse(-1L)

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

  private fun <T> testWithOnDiskByteSink(baseResponse: BaseResponse, restoreFunction: (ByteSink) -> T, testFunction: (T) -> Unit) {
    if (!testFilePath.exists()) {
      testFilePath.createNewFile()
    }

    val response = baseResponse.buildResponse(-1L, OnDiskByteSink.fromFile(testFilePath, 32))
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