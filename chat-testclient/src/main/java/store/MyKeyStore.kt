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
import java.security.KeyStore
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec


class MyKeyStore : Controller() {
  private val keyStoreFile = File("D:\\projects\\data\\chat\\keystore")

  private val PBKDF2WithHmacSHA256 = "PBKDF2WithHmacSHA256"
  private val rootPrivateKeyAlias = "root_private_key"
  private val rootPublicKeyAlias = "root_public_key"
  private val sessionPrivateKey = "session_private_key"
  private val sessionPublicKey = "session_public_key"

  fun init(keyStorePassword: String) {
    val keyStore = getOrCreateKeyStore(keyStorePassword)

    if (!keyStore.containsAlias(rootPrivateKeyAlias)) {
      val keyPair = generateEllipticCurveKeyPair()

      storeKey(keyStorePassword, rootPrivateKeyAlias, String(keyPair.private.encoded()).toCharArray())
      storeKey(keyStorePassword, rootPrivateKeyAlias, String(keyPair.public.encoded()).toCharArray())
    }

    if (!keyStore.containsAlias(sessionPrivateKey)) {
      val keyPair = generateEllipticCurveKeyPair()

      storeKey(keyStorePassword, sessionPrivateKey, String(keyPair.private.encoded()).toCharArray())
      storeKey(keyStorePassword, sessionPublicKey, String(keyPair.public.encoded()).toCharArray())
    }
  }

  private fun getOrCreateKeyStore(keyStorePassword: String): KeyStore {
    try {
      val keyStore = KeyStore.getInstance("BKS", "BC")
      if (hasKeyStoreWithKeys(keyStore)) {
        keyStore.load(FileInputStream(keyStoreFile), keyStorePassword.toCharArray())
      } else {
        keyStore.load(null, null)
        keyStore.store(FileOutputStream(keyStoreFile), keyStorePassword.toCharArray())
      }

      return keyStore
    } catch (error: Exception) {
      throw IllegalStateException("Could not get or create keyStore: ${error.message ?: "No message"}")
    }
  }

  private fun hasKeyStoreWithKeys(keyStore: KeyStore): Boolean {
    if (!keyStoreFile.exists()) {
      return false
    }

    if (!keyStore.containsAlias(rootPrivateKeyAlias)) {
      return false
    }

    if (!keyStore.containsAlias(rootPublicKeyAlias)) {
      return false
    }

    return true
  }

  fun loadKey(keyStorePassword: String, keyAlias: String): ByteArray {
    try {
      val keyStore = getOrCreateKeyStore(keyStorePassword)
      val protectionParameter = KeyStore.PasswordProtection(keyStorePassword.toCharArray())

      val factory = SecretKeyFactory.getInstance(PBKDF2WithHmacSHA256)
      val keyEntry = keyStore.getEntry(keyAlias, protectionParameter) as KeyStore.SecretKeyEntry
      val keySpec = factory.getKeySpec(keyEntry.secretKey, PBEKeySpec::class.java) as PBEKeySpec

      return String(keySpec.password).toByteArray()
    } catch (error: Exception) {
      throw IllegalStateException("Could not load key ${keyAlias} from the keyStore ${error.message ?: "No message"}")
    }
  }

  fun storeKey(keyStorePassword: String, keyAlias: String, key: CharArray) {
    try {
      val keyStore = getOrCreateKeyStore(keyStorePassword)

      val factory = SecretKeyFactory.getInstance(PBKDF2WithHmacSHA256)
      val generatedSecret = factory.generateSecret(PBEKeySpec(key))

      val protectionParameter = KeyStore.PasswordProtection(keyStorePassword.toCharArray())
      keyStore.setEntry(keyAlias, KeyStore.SecretKeyEntry(generatedSecret), protectionParameter)

      FileOutputStream(keyStoreFile).use { fos ->
        keyStore.store(fos, keyStorePassword.toCharArray())
      }

    } catch (error: Exception) {
      throw IllegalStateException("Could not store key ${keyAlias} to the keyStore ${error.message ?: "No message"}")
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
}