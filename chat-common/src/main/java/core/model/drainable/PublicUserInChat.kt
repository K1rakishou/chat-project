package core.model.drainable

import core.Constants
import core.byte_sink.ByteSink
import core.interfaces.CanBeDrainedToSink
import core.interfaces.CanBeRestoredFromSink
import core.interfaces.CanMeasureSizeOfFields
import core.sizeof

class PublicUserInChat(
  val userName: String,
  val rootPublicKey: ByteArray,
  val sessionPublicKey: ByteArray
) : CanMeasureSizeOfFields, CanBeDrainedToSink {

  override fun getSize(): Int {
    return sizeof(userName) + sizeof(rootPublicKey) + sizeof(sessionPublicKey)
  }

  override fun serialize(sink: ByteSink) {
    sink.writeString(userName)
    sink.writeByteArray(rootPublicKey)
    sink.writeByteArray(sessionPublicKey)
  }

  companion object : CanBeRestoredFromSink {
    override fun <T> createFromByteSink(byteSink: ByteSink): T? {
      val userName = byteSink.readString(Constants.maxUserNameLen)
      if (userName == null) {
        return null
      }

      val rootPublicKey = byteSink.readByteArray(Constants.maxEcPublicKeySize)
      if (rootPublicKey == null) {
        return null
      }

      val sessionPublicKey = byteSink.readByteArray(Constants.maxEcPublicKeySize)
      if (sessionPublicKey == null) {
        return null
      }

      return PublicUserInChat(userName, rootPublicKey, sessionPublicKey) as T
    }
  }
}