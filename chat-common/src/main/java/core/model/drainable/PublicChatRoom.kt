package core.model.drainable

import core.Constants
import core.byte_sink.ByteSink
import core.interfaces.CanBeDrainedToSink
import core.interfaces.CanBeRestoredFromSink
import core.interfaces.CanMeasureSizeOfFields
import core.sizeof

class PublicChatRoom(
  val chatRoomName: String,
  val chatRoomImageUrl: String
) : CanMeasureSizeOfFields, CanBeDrainedToSink {

  override fun getSize(): Int {
    return sizeof(chatRoomName) + sizeof(chatRoomImageUrl)
  }

  override fun serialize(sink: ByteSink) {
    sink.writeString(chatRoomName)
    sink.writeString(chatRoomImageUrl)
  }

  companion object : CanBeRestoredFromSink {
    override fun <T> createFromByteSink(byteSink: ByteSink): T? {
      val roomName = byteSink.readString(Constants.maxChatRoomNameLength)
      if (roomName == null) {
        return null
      }

      val chatRoomImageUrl = byteSink.readString(Constants.maxImageUrlLen)
      if (chatRoomImageUrl == null) {
        return null
      }

      return PublicChatRoom(roomName, chatRoomImageUrl) as T
    }
  }
}