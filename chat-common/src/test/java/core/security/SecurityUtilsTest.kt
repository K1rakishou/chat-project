package core.security

import core.security.SecurityUtils.Encryption.chaCha20
import core.security.SecurityUtils.Exchange.calculateAgreement
import core.security.SecurityUtils.Exchange.generateECKeyPair
import core.security.SecurityUtils.Signing.generateSignature
import core.security.SecurityUtils.Signing.verifySignature
import org.bouncycastle.crypto.util.PrivateKeyFactory
import org.bouncycastle.crypto.util.PublicKeyFactory
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.security.Security
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class SecurityUtilsTest {

  @Before
  fun setUp() {
    Security.setProperty("crypto.policy", "unlimited")
    Security.addProvider(BouncyCastleProvider())
  }

  @Test
  fun testECKeyExchange() {
    val senderKeyPair = generateECKeyPair()
    val receiverKeyPair = generateECKeyPair()

    val senderEncodedPublicKey = senderKeyPair.public.encoded
    val receiverEncodedPublicKey = receiverKeyPair.public.encoded

    val senderDecodedPublicKey = PublicKeyFactory.createKey(senderEncodedPublicKey)
    val receiverDecodedPublicKey = PublicKeyFactory.createKey(receiverEncodedPublicKey)

    val agreement1 = calculateAgreement(PrivateKeyFactory.createKey(senderKeyPair.private.encoded), receiverDecodedPublicKey)
    val agreement2 = calculateAgreement(PrivateKeyFactory.createKey(receiverKeyPair.private.encoded), senderDecodedPublicKey)

    assertEquals(agreement1, agreement2)
  }

  @Test
  fun testSignatureVerifying() {
    val senderKeyPair = generateECKeyPair()
    val array = "This is a test string".toByteArray()
    val signature = generateSignature(senderKeyPair.private, array)

    assertTrue(verifySignature(senderKeyPair.public, array, signature))
  }

  @Test
  fun testSignatureVerifyingFailWhenSignatureChanged() {
    val senderKeyPair = generateECKeyPair()
    val array = "This is a test string".toByteArray()
    val signature = generateSignature(senderKeyPair.private, array)

    signature[0] = 0
    signature[1] = 1
    signature[2] = 2

    assertFalse(verifySignature(senderKeyPair.public, array, signature))
  }

  @Test
  fun testSignatureVerifyingFailWhenMessageChanged() {
    val senderKeyPair = generateECKeyPair()
    val array = "This is a test string".toByteArray()
    val signature = generateSignature(senderKeyPair.private, array)

    array[0] = 0
    array[1] = 1
    array[2] = 2

    assertFalse(verifySignature(senderKeyPair.public, array, signature))
  }

  @Test
  fun testChaCha20EncryptionDecryption() {
    val key = "11223344556677889900112233445566".toByteArray()
    val iv = "11223344".toByteArray()
    val testData = "This is a test string".toByteArray()

    val encrypted = chaCha20(key, iv, testData, true)
    val decrypted = chaCha20(key, iv, encrypted, false)

    Assert.assertArrayEquals(testData, decrypted)
  }

  @Test
  fun testChaCha20EncryptionDecryptionWithSmallBuffer() {
    val key = "11223344556677889900112233445566".toByteArray()
    val iv = "11223344".toByteArray()
    val testData = ("This is a test string 11223344556677889900 11223344556677889900 11223344556677889900 " +
      "11223344556677889900 11223344556677889900 11223344556677889900 11223344556677889900").toByteArray()
    val bufferSize = 32

    val encrypted = chaCha20(key, iv, testData, true, bufferSize)
    val decrypted = chaCha20(key, iv, encrypted, false, bufferSize)

    Assert.assertArrayEquals(testData, decrypted)
  }
}