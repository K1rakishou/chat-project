package core.model.drainable

import core.Constants
import core.byte_sink.ByteSink
import core.interfaces.CanBeDrainedToSink
import core.interfaces.CanBeRestoredFromSink
import core.interfaces.CanMeasureSizeOfFields
import core.sizeof

class PublicChatRoom(
  val chatRoomName: String,
  val usersCount: Short
) : CanMeasureSizeOfFields, CanBeDrainedToSink {

  override fun getSize(): Int {
    return sizeof(chatRoomName) + sizeof(usersCount)
  }

  override fun serialize(sink: ByteSink) {
    sink.writeString(chatRoomName)
    sink.writeShort(usersCount)
  }

  companion object : CanBeRestoredFromSink {
    override fun <T> createFromByteSink(byteSink: ByteSink): T? {
      val roomName = byteSink.readString(Constants.maxChatRoomNameLength)
      if (roomName == null) {
        return null
      }

      return PublicChatRoom(roomName, byteSink.readShort()) as T
    }
  }
}