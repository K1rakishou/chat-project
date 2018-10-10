package store

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.security.Security

class MyKeyStoreTest {
  private val myKeyStore = MyKeyStore()
  private val keyStorePassword = "12345"
  private val keyAlias = "test_key_alias"
  private val key = "test_key".toCharArray()

  private fun toCharArray(byteArray: ByteArray): CharArray {
    return String(byteArray).toCharArray()
  }

  @Before
  fun setUp() {
    Security.addProvider(BouncyCastleProvider())
  }

  @After
  fun tearDown() {
    myKeyStore.destroy()
  }

  @Test
  fun testStoreLoadKey() {
    val keyStore = myKeyStore.getOrCreateKeyStore(keyStorePassword)
    myKeyStore.storeKey(keyStorePassword, keyAlias, key)

    assertArrayEquals(key, toCharArray(myKeyStore.loadKey(keyStorePassword, keyAlias)))
  }
}