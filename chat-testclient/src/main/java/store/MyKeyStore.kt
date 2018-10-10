package store

import core.extensions.encoded
import core.security.SecurityUtils
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import tornadofx.Controller
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.IllegalStateException
import java.nio.file.Files
import java.security.KeyStore
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec


class MyKeyStore : Controller() {
  private val type = "BKS"
  private val provider = "BC"

  private val keyStoreFile = File("D:\\projects\\data\\chat\\keystore")
  private val keyStore by lazy { KeyStore.getInstance(type, provider) }

  private val PBKDF2WithHmacSHA256 = "PBKDF2WithHmacSHA256"
  private val rootPrivateKeyAlias = "root_private_key"
  private val rootPublicKeyAlias = "root_public_key"
  private val sessionPrivateKey = "session_private_key"
  private val sessionPublicKey = "session_public_key"

  fun init(keyStorePassword: String) {
    val keyStore = getOrCreateKeyStore(keyStorePassword)

    if (!keyStore.containsAlias(rootPrivateKeyAlias)) {
      val keyPair = generateEllipticCurveKeyPair()

      //I wish I could just cast ByteArray to CharArray and vice versa
      storeKey(keyStorePassword, rootPrivateKeyAlias, String(keyPair.private.encoded()).toCharArray())
      storeKey(keyStorePassword, rootPrivateKeyAlias, String(keyPair.public.encoded()).toCharArray())
    }

    if (!keyStore.containsAlias(sessionPrivateKey)) {
      val keyPair = generateEllipticCurveKeyPair()

      storeKey(keyStorePassword, sessionPrivateKey, String(keyPair.private.encoded()).toCharArray())
      storeKey(keyStorePassword, sessionPublicKey, String(keyPair.public.encoded()).toCharArray())
    }
  }

  fun getOrCreateKeyStore(keyStorePassword: String): KeyStore {
    val password = keyStorePassword.toCharArray()
    keyStore.load(null, password)

    if (keyStoreFile.exists()) {
      FileInputStream(keyStoreFile).use { fis ->
        keyStore.load(fis, password)
      }
    } else {
      keyStore.load(null, null)

      FileOutputStream(keyStoreFile).use { fos ->
        keyStore.store(fos, password)
      }
    }

    return keyStore
  }

  fun loadKey(keyStorePassword: String, keyAlias: String): ByteArray {
    val password = keyStorePassword.toCharArray()
    keyStore.load(null, password)

    FileInputStream(keyStoreFile).use { fis ->
      keyStore.load(fis, password)
    }

    if (!keyStore.containsAlias(keyAlias)) {
      throw IllegalStateException("keyStore does not contain key with alias $keyAlias")
    }

    val keyEntry = keyStore.getEntry(
      keyAlias,
      KeyStore.PasswordProtection(password)
    ) as KeyStore.SecretKeyEntry

    val factory = SecretKeyFactory.getInstance(PBKDF2WithHmacSHA256)
    val keySpec = factory.getKeySpec(keyEntry.secretKey, PBEKeySpec::class.java) as PBEKeySpec

    return String(keySpec.password).toByteArray()
  }

  fun storeKey(keyStorePassword: String, keyAlias: String, key: CharArray) {
    val password = keyStorePassword.toCharArray()
    keyStore.load(null, password)

    val factory = SecretKeyFactory.getInstance(PBKDF2WithHmacSHA256)
    val generatedSecret = factory.generateSecret(PBEKeySpec(key))

    keyStore.setEntry(
      keyAlias,
      KeyStore.SecretKeyEntry(generatedSecret),
      KeyStore.PasswordProtection(password)
    )

    FileOutputStream(keyStoreFile).use { fos ->
      keyStore.store(fos, password)
      fos.flush()
    }
  }

  fun getRootPublicKeyEncoded(keyStorePassword: String): ByteArray {
    return loadKey(keyStorePassword, rootPublicKeyAlias)
  }

  fun getSessionPublicKeyEncoded(keyStorePassword: String): ByteArray {
    return loadKey(keyStorePassword, sessionPublicKey)
  }

  fun getPrivateKey(): AsymmetricKeyParameter {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  fun getSharedSecret(receiverId: String): ByteArray {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  private fun generateEllipticCurveKeyPair(): AsymmetricCipherKeyPair {
    return SecurityUtils.Exchange.generateECKeyPair()
  }

  fun destroy() {
    Files.deleteIfExists(keyStoreFile.toPath())
  }
}