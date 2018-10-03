package core

import core.byte_sink.ByteSink
import core.interfaces.CanBeDrainedToSink
import core.interfaces.CanMeasureSizeOfFields

class PublicUserInChat(
  val userName: String,
  val ecPublicKey: ByteArray
) : CanMeasureSizeOfFields, CanBeDrainedToSink {

  override fun getSize(): Int {
    return sizeof(userName) + sizeof(ecPublicKey)
  }

  override fun serialize(sink: ByteSink) {
    sink.writeString(userName)
    sink.writeByteArray(ecPublicKey)
  }

  override fun <T> deserialize(sink: ByteSink): T {
    val userName = sink.readString()!!
    val ecPublicKey = sink.readByteArray()

    return PublicUserInChat(userName, ecPublicKey) as T
  }
}