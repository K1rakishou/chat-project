package core.model.drainable

import core.byte_sink.ByteSink
import core.interfaces.CanBeDrainedToSink
import core.interfaces.CanBeRestoredFromSink
import core.interfaces.CanMeasureSizeOfFields
import core.sizeof

class PublicChatRoom(
  val roomName: String,
  val usersCount: Short
) : CanMeasureSizeOfFields, CanBeDrainedToSink {

  override fun getSize(): Int {
    return sizeof(roomName) + sizeof(usersCount)
  }

  override fun serialize(sink: ByteSink) {
    sink.writeString(roomName)
    sink.writeShort(usersCount)
  }

  companion object : CanBeRestoredFromSink {
    override fun <T> createFromByteSink(byteSink: ByteSink): T? {
      val roomName = byteSink.readString()
      if (roomName == null) {
        return null
      }

      return PublicChatRoom(roomName, byteSink.readShort()) as T
    }
  }
}