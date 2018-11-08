package core

object Constants {
  const val arrayChunkSize = 4096
  const val maxInMemoryByteSinkSize = arrayChunkSize

  const val maxRoomHistoryMessagesCount = 100
  const val maxUsersInRoomCount = 100
  const val maxChatRoomsCount = Short.MAX_VALUE.toInt()

  const val minChatRoomSearchLen = 4
  const val minChatRoomNameLen = 5
  const val maxChatRoomNameLength = 128
  const val minChatRoomPasswordLen = 8
  const val maxChatRoomPasswordLen = 48
  const val maxChatRoomPasswordHashLen = 96
  const val maxChatRoomImageUrlLen = 64

  const val minUserNameLen = 3
  const val maxUserNameLen = 64
  const val maxTextMessageLen = 2048
  const val maxFoundChatRooms = 10
}