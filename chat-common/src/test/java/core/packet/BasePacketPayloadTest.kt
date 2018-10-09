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
import kotlin.test.assertTrue

open class BasePacketPayloadTest {
  private val testFilePath = File("D:\\projects\\data\\chat\\test_file")

  protected fun <T> testPayload(basePacket: BasePacket, restoreFunction: (ByteSink) -> T, testFunction: (T) -> Unit) {
    when (basePacket) {
      is UnencryptedPacket -> {
        testWithInMemoryByteSink(basePacket, restoreFunction, testFunction)
        testWithOnDiskMemoryByteSink(basePacket, restoreFunction, testFunction)
      }
      is EncryptedPacket -> {
        testWithInMemoryByteSink(basePacket, restoreFunction, testFunction)
      }
      else -> throw IllegalStateException("Not implemented for ${basePacket::class}")
    }
  }

  private fun <T> testWithInMemoryByteSink(basePacket: UnencryptedPacket, restoreFunction: (ByteSink) -> T, testFunction: (T) -> Unit) {
    val response = PacketBuilder.buildUnencryptedPacket(basePacket, InMemoryByteSink.createWithInitialSize(basePacket.getPayloadSize()))
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

  private fun <T> testWithOnDiskMemoryByteSink(basePacket: UnencryptedPacket, restoreFunction: (ByteSink) -> T, testFunction: (T) -> Unit) {
    if (!testFilePath.exists()) {
      testFilePath.createNewFile()
    }

    val response = PacketBuilder.buildUnencryptedPacket(basePacket, OnDiskByteSink.fromFile(testFilePath, 32))
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

  private fun <T> testWithInMemoryByteSink(basePacket: EncryptedPacket, restoreFunction: (ByteSink) -> T, testFunction: (T) -> Unit) {
    val keys = SecurityUtils.Exchange.generateECKeyPair()
    val sharedSecret = "11223344556677881122334455667788".toByteArray()

    val bs = InMemoryByteSink.createWithInitialSize(basePacket.getPayloadSize())
    basePacket.toByteSink(bs)
    val originalBytes = bs.getArray()

    val response = PacketBuilder.buildEncryptedPacket(keys.private, sharedSecret, basePacket, bs)
    val bodySize = response.bodySize

    (response.packetBody.bodyByteSink as InMemoryByteSink).use { byteSink ->
      val _randomBytes = byteSink.readByteArray(16) ?: throw NullPointerException("_randomBytes is null")
      val _iv = byteSink.readByteArray(24) ?: throw NullPointerException("_iv is null")
      val _signature = byteSink.readByteArray(75) ?: throw NullPointerException("_signature is null")

      val calculatedBodySize = byteSink.getWriterPosition() - byteSink.getReaderPosition()
      val responseBytesHex = byteSink.readByteArrayRaw(byteSink.getReaderPosition(), calculatedBodySize)

      assertEquals(bodySize, calculatedBodySize)
      assertEquals(calculatedBodySize, responseBytesHex.size)
      assertTrue(SecurityUtils.Signing.verifySignature(keys.public, responseBytesHex, _signature))

      SecurityUtils.Encryption.xSalsa20Decrypt(sharedSecret, _iv, byteSink, byteSink.getReaderPosition() + calculatedBodySize, byteSink.getReaderPosition())
      assertArrayEquals(originalBytes, byteSink.getArray(byteSink.getReaderPosition(), byteSink.getWriterPosition()))

      val restoredResponse = restoreFunction(byteSink)
      testFunction(restoredResponse)
    }
  }
}