package controller

import core.security.SecurityUtils
import tornadofx.Controller
import java.security.KeyPair

class KeyStore : Controller() {
  private var myKeys: MyKeys? = null

  fun generateKeys() {
    val ecKeyPair = SecurityUtils.Exchange.generateECKeyPair()
    val publicKeyEncoded = ecKeyPair.public.encoded

    myKeys = MyKeys(ecKeyPair, publicKeyEncoded)
  }

  fun getMyPublicKeyEncoded(): ByteArray {
    requireNotNull(myKeys)

    return myKeys!!.publicKeyEncoded
  }

  fun areKeysGenerated(): Boolean {
    return myKeys != null
  }

//  fun storeKeyPair() {
//    val keystore = KeyStore.getInstance("BKS", "BC")
//  }

  class MyKeys(
    val ecKeyPair: KeyPair,
    val publicKeyEncoded: ByteArray
  )
}