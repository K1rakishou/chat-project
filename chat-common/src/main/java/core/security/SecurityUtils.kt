package core.security

import org.bouncycastle.crypto.agreement.ECDHBasicAgreement
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.engines.ChaChaEngine
import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import org.bouncycastle.jce.spec.ECParameterSpec
import java.math.BigInteger
import java.security.*
import java.security.spec.X509EncodedKeySpec


object SecurityUtils {
  const val Curve25519 = "Curve25519"
  const val SHA384withECDSA = "SHA384withECDSA"
  const val ECDSA = "ECDSA"

  object Encryption {
    fun chaCha20(key: ByteArray, iv: ByteArray, inBuffer: ByteArray, doEncryption: Boolean, tempBufferSize: Int = 4096): ByteArray {
      require(key.size == 32)
      require(iv.size == 8)

      val cp = KeyParameter(key)
      val params = ParametersWithIV(cp, iv)
      val engine = ChaChaEngine()
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

}