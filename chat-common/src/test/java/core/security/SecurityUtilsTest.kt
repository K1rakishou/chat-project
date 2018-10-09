package core.security

import core.byte_sink.InMemoryByteSink
import core.extensions.encoded
import core.extensions.toAsymmetricKeyParameter
import core.extensions.toHex
import core.security.SecurityUtils.Encryption.xSalsa20Decrypt
import core.security.SecurityUtils.Encryption.xSalsa20Encrypt
import core.security.SecurityUtils.Exchange.calculateAgreement
import core.security.SecurityUtils.Exchange.generateECKeyPair
import core.security.SecurityUtils.Signing.generateSignature
import core.security.SecurityUtils.Signing.verifySignature
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import java.security.Security
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class SecurityUtilsTest {

  @Before
  fun setUp() {
    Security.addProvider(BouncyCastleProvider())
  }

  @Test
  fun testECKeyExchange() {
    val senderKeyPair = generateECKeyPair()
    val receiverKeyPair = generateECKeyPair()

    val senderEncodedPublicKey = senderKeyPair.public.encoded()
    val receiverEncodedPublicKey =  receiverKeyPair.public.encoded()

    val senderDecodedPublicKey = senderEncodedPublicKey.toAsymmetricKeyParameter()
    val receiverDecodedPublicKey = receiverEncodedPublicKey.toAsymmetricKeyParameter()

    val agreement1 = calculateAgreement(senderKeyPair.private, receiverDecodedPublicKey)
    val agreement2 = calculateAgreement(receiverKeyPair.private, senderDecodedPublicKey)

    assertEquals(agreement1, agreement2)
  }

  @Test
  fun testSignatureVerifying() {
    val senderKeyPair = generateECKeyPair()
    val array = "This is a test string".toByteArray()
    val signature = generateSignature(senderKeyPair.private, array)!!

    assertTrue(verifySignature(senderKeyPair.public, array, signature))
  }

  @Test
  fun testSignatureVerifyingFailWhenSignatureChanged() {
    val senderKeyPair = generateECKeyPair()
    val array = "This is a test string".toByteArray()
    val signature = generateSignature(senderKeyPair.private, array)!!

    signature[0] = 0
    signature[1] = 1
    signature[2] = 2

    assertFalse(verifySignature(senderKeyPair.public, array, signature))
  }

  @Test
  fun testSignatureVerifyingFailWhenMessageChanged() {
    val senderKeyPair = generateECKeyPair()
    val array = "This is a test string".toByteArray()
    val signature = generateSignature(senderKeyPair.private, array)!!

    array[0] = 0
    array[1] = 1
    array[2] = 2

    assertFalse(verifySignature(senderKeyPair.public, array, signature))
  }

  @Test
  fun testXSalsa20EncryptionDecryptionFewTimes() {
    val key = "11223344556677889900112233445566".toByteArray()
    val iv = "112233445566778899001122".toByteArray()
    val byteSink = InMemoryByteSink.createWithInitialSize(32)
    byteSink.writeString("This is a test string")

    val original = byteSink.getArray()

    kotlin.run {
      xSalsa20Encrypt(key, iv, byteSink, byteSink.getWriterPosition())
      xSalsa20Decrypt(key, iv, byteSink, byteSink.getWriterPosition())

      val decrypted = byteSink.getArray()
      assertArrayEquals(original, decrypted)
    }

    kotlin.run {
      xSalsa20Encrypt(key, iv, byteSink, byteSink.getWriterPosition())
      xSalsa20Decrypt(key, iv, byteSink, byteSink.getWriterPosition())

      val decrypted = byteSink.getArray()
      assertArrayEquals(original, decrypted)
    }

    kotlin.run {
      xSalsa20Encrypt(key, iv, byteSink, byteSink.getWriterPosition())
      xSalsa20Decrypt(key, iv, byteSink, byteSink.getWriterPosition())

      val decrypted = byteSink.getArray()
      assertArrayEquals(original, decrypted)
    }

    kotlin.run {
      xSalsa20Encrypt(key, iv, byteSink, byteSink.getWriterPosition())
      xSalsa20Decrypt(key, iv, byteSink, byteSink.getWriterPosition())

      val decrypted = byteSink.getArray()
      assertArrayEquals(original, decrypted)
    }
  }

  @Test
  fun testSha3_384() {
    val expectedHash = "d31c6a449362712e691534004552c29ce9ce946be8b94fb2fa8dcef52d861d607fd09b804d0cbf9604e4b7c8db2fb73c".toUpperCase()

    assertEquals(expectedHash, SecurityUtils.Hashing.sha3("test string".toByteArray()).toHex())
    assertEquals(expectedHash, SecurityUtils.Hashing.sha3("test string".toByteArray()).toHex())
    assertEquals(expectedHash, SecurityUtils.Hashing.sha3("test string".toByteArray()).toHex())
    assertEquals(expectedHash, SecurityUtils.Hashing.sha3("test string".toByteArray()).toHex())
  }
}