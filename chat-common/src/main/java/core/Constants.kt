package core

object Constants {
  const val maxInMemoryByteSinkSize = 4096

  const val maxRoomHistoryMessagesCount = 100
  const val maxUsersInRoomCount = 100
  const val maxChatRoomsCount = Short.MAX_VALUE.toInt()
  const val maxChatRoomNameLength = 128
  const val maxChatRoomPasswordHash = 256
  const val maxEcPublicKeySize = 1024
  const val maxUserNameLen = 64
  const val maxTextMessageLen = 2048
}