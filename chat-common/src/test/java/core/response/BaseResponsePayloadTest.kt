package core.response

import core.byte_sink.ByteSink
import core.byte_sink.InMemoryByteSink
import core.byte_sink.OnDiskByteSink
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

open class BaseResponsePayloadTest {
  private val responseBuilder = ResponseBuilder()
  private val testFilePath = File("D:\\projects\\data\\chat\\test_file")

  protected fun <T> testPayload(baseResponse: BaseResponse, restoreFunction: (ByteSink) -> T, testFunction: (T) -> Unit) {
    testWithInMemoryByteSink(baseResponse, restoreFunction, testFunction)
    testWithOnDiskByteSink(baseResponse, restoreFunction, testFunction)
  }

  private fun <T> testWithInMemoryByteSink(baseResponse: BaseResponse, restoreFunction: (ByteSink) -> T, testFunction: (T) -> Unit) {
    val response = responseBuilder.buildResponse(baseResponse, InMemoryByteSink.createWithInitialSize(baseResponse.getPayloadSize()))
    val bodySize = response.bodySize

    (response.bodyByteSink as InMemoryByteSink).use { byteSink ->
      val calculatedBodySize = byteSink.getWriterPosition()
      val responseBytesHex = byteSink.getArray()

      assertEquals(bodySize, calculatedBodySize)
      assertEquals(calculatedBodySize, responseBytesHex.size)

      val restoredResponse = restoreFunction(byteSink)
      testFunction(restoredResponse)
    }
  }

  private fun <T> testWithOnDiskByteSink(baseResponse: BaseResponse, restoreFunction: (ByteSink) -> T, testFunction: (T) -> Unit) {
    if (!testFilePath.exists()) {
      testFilePath.createNewFile()
    }

    val response = responseBuilder.buildResponse(baseResponse, OnDiskByteSink.fromFile(testFilePath, 32))
    val bodySize = response.bodySize

    (response.bodyByteSink as OnDiskByteSink).use { byteSink ->
      val calculatedBodySize = byteSink.getWriterPosition()
      val responseBytesHex = byteSink.getArray()

      assertEquals(bodySize, calculatedBodySize)
      assertEquals(calculatedBodySize, responseBytesHex.size)

      val restoredResponse = restoreFunction(byteSink)
      testFunction(restoredResponse)
    }
  }

}