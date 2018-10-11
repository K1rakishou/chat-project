package core.model.drainable

import core.Constants
import core.byte_sink.ByteSink
import core.interfaces.CanBeDrainedToSink
import core.interfaces.CanBeRestoredFromSink
import core.interfaces.CanMeasureSizeOfFields
import core.sizeof

class PublicUserInChat(
  val userName: String
) : CanMeasureSizeOfFields, CanBeDrainedToSink {

  override fun getSize(): Int {
    return sizeof(userName)
  }

  override fun serialize(sink: ByteSink) {
    sink.writeString(userName)
  }

  companion object : CanBeRestoredFromSink {
    override fun <T> createFromByteSink(byteSink: ByteSink): T? {
      val userName = byteSink.readString(Constants.maxUserNameLen)
      if (userName == null) {
        return null
      }

      return PublicUserInChat(userName) as T
    }
  }
}