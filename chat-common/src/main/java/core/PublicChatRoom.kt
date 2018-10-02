package core

import core.byte_sink.ByteSink
import core.interfaces.CanMeasureSizeOfFields

class PublicChatRoom(
  val roomName: String,
  val usersCount: Short
) : CanMeasureSizeOfFields {

  override fun getSize(): Int {
    return sizeof(roomName) + sizeof(usersCount)
  }

  fun toByteSink(byteSink: ByteSink) {
    byteSink.writeString(roomName)
    byteSink.writeShort(usersCount)
  }

  companion object {
    fun fromByteSink(byteSink: ByteSink): PublicChatRoom {
      return PublicChatRoom(byteSink.readString()!!, byteSink.readShort())
    }
  }
}