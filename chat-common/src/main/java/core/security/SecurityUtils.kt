package core.security

import core.byte_sink.ByteSink
import core.extensions.toHex
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.DERSequenceGenerator
import org.bouncycastle.asn1.DLSequence
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement
import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.engines.XSalsa20Engine
import org.bouncycastle.crypto.generators.ECKeyPairGenerator
import org.bouncycastle.crypto.params.*
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.crypto.signers.HMacDSAKCalculator
import org.bouncycastle.jcajce.provider.digest.SHA3
import org.bouncycastle.jce.ECNamedCurveTable
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.security.SecureRandom


object SecurityUtils {
  const val Curve25519 = "Curve25519"

  private val alphabet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_-"
  private val secureRandom = SecureRandom.getInstanceStrong()

  object Generator {

    fun generateRandomByteArray(len: Int): ByteArray {
      val array = ByteArray(len)
      secureRandom.nextBytes(array)

      return array
    }

    fun generateRandomString(len: Int): String {
      val array = ByteArray(len)
      secureRandom.nextBytes(array)

      val sb = StringBuilder()
      val alphabetLen = alphabet.length

      for (i in 0 until len) {
        sb.append(alphabet[Math.abs(array[i] % alphabetLen)])
      }

      return sb.toString()
    }
  }

  object Encryption {
    private val engine = XSalsa20Engine()
    const val ivLen = 24
    const val keyLen = 32

    private enum class Mode {
      Encryption,
      Decryption
    }

    fun xSalsa20Encrypt(key: ByteArray, iv: ByteArray, byteSink: ByteSink, byteSinkSize: Int, offset: Int = 0) {
      xSalsa20(key, iv, byteSink, byteSinkSize, offset, Mode.Encryption)
    }

    fun xSalsa20Decrypt(key: ByteArray, iv: ByteArray, byteSink: ByteSink, byteSinkSize: Int, offset: Int = 0) {
      xSalsa20(key, iv, byteSink, byteSinkSize, offset, Mode.Decryption)
    }

    private fun xSalsa20(key: ByteArray, iv: ByteArray, byteSink: ByteSink, byteSinkSize: Int, byteSinkOffset: Int, mode: Mode) {
      require(iv.size == ivLen) { "iv.size != $ivLen (${iv.size})" }
      require(key.size == keyLen) { "key.size != $keyLen (${key.size})" }
      require(byteSinkOffset < byteSink.getWriterPosition()) { "byteSinkOffset ($byteSinkOffset) >= byteSink.getWriterPosition() (${byteSink.getWriterPosition()})" }

      val doEncryption = when (mode) {
        SecurityUtils.Encryption.Mode.Encryption -> true
        SecurityUtils.Encryption.Mode.Decryption -> false
      }

      val params = ParametersWithIV(KeyParameter(key), iv)
      engine.init(doEncryption, params)

      val defaultChunkSize = 4096
      val chunkSize = if (byteSinkSize < defaultChunkSize) {
        byteSinkSize
      } else {
        defaultChunkSize
      }

      require(byteSinkOffset < byteSinkSize)

      for (offset in byteSinkOffset until byteSinkSize step chunkSize) {
        val size = Math.min(byteSinkSize - offset, chunkSize)

        val chunk = byteSink.readByteArrayRaw(offset, size)
        val encryptedChunk = ByteArray(size)
        engine.processBytes(chunk, 0, size, encryptedChunk, 0)

        byteSink.rewriteByteArrayRaw(offset, encryptedChunk)
      }
    }
  }

  object Exchange {
    private val ecCurve = ECNamedCurveTable.getParameterSpec(SecurityUtils.Curve25519)

    fun generateECKeyPair(): AsymmetricCipherKeyPair {
      val ecDomainParam = ECDomainParameters(ecCurve.curve, ecCurve.g, ecCurve.n, ecCurve.h, ecCurve.seed)
      val kp = ECKeyPairGenerator()
        .apply { init(ECKeyGenerationParameters(ecDomainParam, secureRandom)) }

      return kp.generateKeyPair()
    }

    fun calculateAgreement(privateKey: AsymmetricKeyParameter, publicKey: AsymmetricKeyParameter): BigInteger {
      val senderAgreement = ECDHBasicAgreement()
      senderAgreement.init(privateKey)

      return senderAgreement.calculateAgreement(publicKey)
    }
  }

  object Signing {
    private val signer = ECDSASigner(HMacDSAKCalculator(SHA512Digest()))

    fun generateSignature(ecPrivateKey: AsymmetricKeyParameter, message: ByteArray): ByteArray? {
      signer.init(true, ecPrivateKey)

      val signature = signer.generateSignature(message)
      val baos = ByteArrayOutputStream()

      return try {
        val seq = DERSequenceGenerator(baos)
        seq.addObject(ASN1Integer(signature[0]))
        seq.addObject(ASN1Integer(signature[1]))
        seq.close()

        baos.toByteArray()
      } catch (e: IOException) {
        e.printStackTrace()
        null
      } finally {
        try {
          baos.close()
        } catch (ignored: IOException) {
        }
      }
    }

    fun verifySignature(ecPublicKey: AsymmetricKeyParameter, input: ByteArray, signature: ByteArray): Boolean {
      val asn1 = ASN1InputStream(signature)

      try {
        signer.init(false, ecPublicKey)

        val seq = asn1.readObject() as DLSequence
        val r = (seq.getObjectAt(0) as ASN1Integer).positiveValue
        val s = (seq.getObjectAt(1) as ASN1Integer).positiveValue

        return signer.verifySignature(input, r, s);
      } catch (e: Exception) {
        e.printStackTrace()
        return false
      } finally {
        try {
          asn1.close()
        } catch (ignored: IOException) {
        }
      }
    }
  }

  object Hashing {
    private val sha3 = SHA3.DigestSHA3(384)

    fun sha3(data: ByteArray): String {
      return sha3.digest(data).toHex().toUpperCase()
    }

    fun sha3(data: String): String {
      return sha3(data.toByteArray())
    }
  }

}