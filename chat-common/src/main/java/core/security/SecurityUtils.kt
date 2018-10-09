package core.security

import core.byte_sink.ByteSink
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.DERSequenceGenerator
import org.bouncycastle.asn1.DLSequence
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement
import org.bouncycastle.crypto.engines.XSalsa20Engine
import org.bouncycastle.crypto.generators.ECKeyPairGenerator
import org.bouncycastle.crypto.params.*
import org.bouncycastle.crypto.signers.ECDSASigner
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
    fun generateRandomString(len: Int): String {
      val bytes = ByteArray(len)
      secureRandom.nextBytes(bytes)

      val sb = StringBuilder()
      val alphabetLen = alphabet.length

      for (i in 0 until len) {
        sb.append(alphabet[Math.abs(bytes[i] % alphabetLen)])
      }

      return sb.toString()
    }
  }

  object Encryption {
    private val engine = XSalsa20Engine()

    private enum class Mode {
      Encryption,
      Decryption
    }

    fun xSalsa20Encrypt(key: ByteArray, iv: ByteArray, byteSink: ByteSink, byteSinkSize: Int) {
      xSalsa20(key, iv, byteSink, byteSinkSize, Mode.Encryption)
    }

    fun xSalsa20Decrypt(key: ByteArray, iv: ByteArray, byteSink: ByteSink, byteSinkSize: Int) {
      xSalsa20(key, iv, byteSink, byteSinkSize, Mode.Decryption)
    }

    private fun xSalsa20(key: ByteArray, iv: ByteArray, byteSink: ByteSink, byteSinkSize: Int, mode: Mode) {
      require(iv.size == 24)
      require(key.size == 32)

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

      for (offset in 0 until byteSinkSize step chunkSize) {
        val size = Math.min(byteSinkSize - offset, chunkSize)

        val chunk = byteSink.readByteArrayRaw(offset, size)
        val encryptedChunk = ByteArray(size)
        engine.processBytes(chunk, 0, size, encryptedChunk, 0)

        byteSink.writeByteArrayRaw(offset, encryptedChunk)
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
    private val signer = ECDSASigner()

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

    fun sha3(data: ByteArray): ByteArray {
      return sha3.digest(data)
    }
  }

}