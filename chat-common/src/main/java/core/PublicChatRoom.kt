package core

import core.interfaces.CanMeasureSizeOfFields

class PublicChatRoom(
  private val roomName: String,
  private val usersCount: Short
) : CanMeasureSizeOfFields {

  override fun getSize(): Int {
    return sizeof(roomName) + sizeof(usersCount)
  }

  fun toByteArray(byteArray: PositionAwareByteArray) {
    byteArray.writeString(roomName)
    byteArray.writeShort(usersCount)
  }

  companion object {
    fun fromByteArray(byteArray: PositionAwareByteArray): PublicChatRoom {
      return PublicChatRoom(byteArray.readString()!!, byteArray.readShort())
    }
  }
}