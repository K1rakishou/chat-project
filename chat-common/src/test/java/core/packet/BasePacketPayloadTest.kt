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
  private val testFilePath = File("D:\\projects\\data\\chat\\test_file")

  protected fun <T> testPayload(basePacket: BasePacket, restoreFunction: (ByteSink) -> T, testFunction: (T) -> Unit) {
    when (basePacket) {
      is UnencryptedPacket -> {
        kotlin.run {
          testUnencryptedPacket(basePacket, InMemoryByteSink.createWithInitialSize(basePacket.getPayloadSize()), restoreFunction, testFunction)
        }
        kotlin.run {
          if (!testFilePath.exists()) {
            testFilePath.createNewFile()
          }

          testUnencryptedPacket(basePacket, OnDiskByteSink.fromFile(testFilePath, basePacket.getPayloadSize()), restoreFunction, testFunction)
        }
      }
      is EncryptedPacket -> {
        kotlin.run {
          testEncryptedPacket(basePacket, InMemoryByteSink.createWithInitialSize(basePacket.getPayloadSize()), restoreFunction, testFunction)
        }
        kotlin.run {
          if (!testFilePath.exists()) {
            testFilePath.createNewFile()
          }

          testEncryptedPacket(basePacket, OnDiskByteSink.fromFile(testFilePath, basePacket.getPayloadSize()), restoreFunction, testFunction)
        }
      }
      else -> throw IllegalStateException("Not implemented for ${basePacket::class}")
    }
  }

  private fun <T> testUnencryptedPacket(basePacket: UnencryptedPacket, _byteSink: ByteSink, restoreFunction: (ByteSink) -> T, testFunction: (T) -> Unit) {
    val response = PacketBuilder.buildUnencryptedPacket(basePacket, _byteSink)

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

  private fun <T> testEncryptedPacket(basePacket: EncryptedPacket, byteSink: ByteSink, restoreFunction: (ByteSink) -> T, testFunction: (T) -> Unit) {
    byteSink.use { bs ->
      val keys = SecurityUtils.Exchange.generateECKeyPair()
      val sharedSecret = "11223344556677881122334455667788".toByteArray()

      basePacket.toByteSink(bs)
      val originalBytes = bs.getArray()

      val response = PacketBuilder.buildEncryptedPacket(keys.private, sharedSecret, basePacket, bs)

      assertNotNull(response)
      //TODO
      //can't wait for kotlin's 1.3 contracts to get rid of this
      response!!

      val bodySize = response.bodySize

      response.packetBody.bodyByteSink.use { byteSink ->
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

      assertTrue(response.packetBody.bodyByteSink.isClosed())
    }

    assertTrue(byteSink.isClosed())
  }
}