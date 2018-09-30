package core.security

import org.bouncycastle.crypto.agreement.ECDHBasicAgreement
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.engines.ChaChaEngine
import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import org.bouncycastle.jcajce.provider.digest.SHA3
import org.bouncycastle.jce.spec.ECParameterSpec
import java.math.BigInteger
import java.security.*
import java.security.spec.X509EncodedKeySpec


object SecurityUtils {
  const val Curve25519 = "Curve25519"
  const val SHA384withECDSA = "SHA384withECDSA"
  const val ECDSA = "ECDSA"

  private val alphabet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_-"
  private val secureRandom = SecureRandom.getInstanceStrong()

  object Generation {
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
    private enum class Mode {
      Encryption,
      Decryption
    }

    fun chaCha20Encrypt(key: ByteArray, iv: ByteArray, inBuffer: ByteArray, tempBufferSize: Int = 4096): ByteArray {
      return chaCha20(key, iv, inBuffer, Mode.Encryption, tempBufferSize)
    }

    fun chaCha20Decrypt(key: ByteArray, iv: ByteArray, inBuffer: ByteArray, tempBufferSize: Int = 4096): ByteArray {
      return chaCha20(key, iv, inBuffer, Mode.Decryption, tempBufferSize)
    }

    private fun chaCha20(key: ByteArray, iv: ByteArray, inBuffer: ByteArray, mode: Mode, tempBufferSize: Int = 4096): ByteArray {
      require(key.size == 32)
      require(iv.size == 8)

      val cp = KeyParameter(key)
      val params = ParametersWithIV(cp, iv)
      val engine = ChaChaEngine()
      val doEncryption = when (mode) {
        SecurityUtils.Encryption.Mode.Encryption -> true
        SecurityUtils.Encryption.Mode.Decryption -> false
      }

      engine.init(doEncryption, params)

      val outBufferList = mutableListOf<ByteArray>()

      for (offset in 0 until inBuffer.size step tempBufferSize) {
        val size = Math.min(inBuffer.size - offset, tempBufferSize)
        val tempBuffer = ByteArray(size)

        engine.processBytes(inBuffer, offset, size, tempBuffer, 0)
        outBufferList += tempBuffer
      }

      val outBuffer = ByteArray(inBuffer.size)
      var offset = 0

      for (buffer in outBufferList) {
        System.arraycopy(buffer, 0, outBuffer, offset, buffer.size)
        offset += buffer.size
      }

      return outBuffer
    }
  }

  object Exchange {
    fun generateECKeyPair(): KeyPair {
      val kp = KeyPairGenerator.getInstance(ECDSA)
      val ecP = CustomNamedCurves.getByName(SecurityUtils.Curve25519)
      val ecSpec = ECParameterSpec(ecP.curve, ecP.g, ecP.n, ecP.h, ecP.seed)

      kp.initialize(ecSpec)
      return kp.generateKeyPair()
    }

    fun decodePublicKey(encodedPubKey: ByteArray): PublicKey {
      val kf = KeyFactory.getInstance(ECDSA)
      return kf.generatePublic(X509EncodedKeySpec(encodedPubKey))
    }

//    fun decodePrivateKey(encodedPrivKey: ByteArray): PrivateKey {
//      val kf = KeyFactory.getInstance(ECDSA)
//      return kf.generatePrivate(PKCS8EncodedKeySpec(encodedPrivKey))
//    }

    fun calculateAgreement(privateKey: AsymmetricKeyParameter, publicKey: AsymmetricKeyParameter): BigInteger {
      val senderAgreement = ECDHBasicAgreement()
      senderAgreement.init(privateKey)

      return senderAgreement.calculateAgreement(publicKey)
    }
  }

  object Signing {
    fun generateSignature(ecPrivate: PrivateKey, input: ByteArray): ByteArray {
      val signature = Signature.getInstance(SHA384withECDSA, "BC")
      signature.initSign(ecPrivate)
      signature.update(input)
      return signature.sign()
    }

    fun verifySignature(ecPublicKey: PublicKey, input: ByteArray, encSignature: ByteArray): Boolean {
      return try {
        val signature = Signature.getInstance(SHA384withECDSA, "BC")
        signature.initVerify(ecPublicKey)
        signature.update(input)
        signature.verify(encSignature)
      } catch (error: SignatureException) {
        false
      }
    }
  }

  object Hashing {
    fun sha3(data: ByteArray): ByteArray {
      val sha3 = SHA3.DigestSHA3(384)
      return sha3.digest(data)
    }
  }

}