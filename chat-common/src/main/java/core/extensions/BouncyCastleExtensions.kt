package core.extensions

import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import org.bouncycastle.crypto.util.PublicKeyFactory
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory

fun AsymmetricKeyParameter.encoded(): ByteArray {
  return SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(this).encoded
}

fun ByteArray.toAsymmetricKeyParameter(): AsymmetricKeyParameter {
  return PublicKeyFactory.createKey(this)
}