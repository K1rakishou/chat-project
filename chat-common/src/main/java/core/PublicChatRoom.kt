package core

import core.byte_sink.InMemoryByteSink
import core.interfaces.CanMeasureSizeOfFields

class PublicChatRoom(
  val roomName: String,
  val usersCount: Short
) : CanMeasureSizeOfFields {

  override fun getSize(): Int {
    return sizeof(roomName) + sizeof(usersCount)
  }

  fun toByteSink(byteSink: InMemoryByteSink) {
    byteSink.writeString(roomName)
    byteSink.writeShort(usersCount)
  }

  companion object {
    fun fromByteSink(byteSink: InMemoryByteSink): PublicChatRoom {
      return PublicChatRoom(byteSink.readString()!!, byteSink.readShort())
    }
  }
}