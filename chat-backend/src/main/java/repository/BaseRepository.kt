package repository

import core.ChatRoom

open class BaseRepository {
//  val hzInstance: HazelcastInstance
//
//  init {
//    val config = Config()
//    config.mapConfigs.put(chatRoomsMap, chatRoomsMapConfig())
//
//    hzInstance = Hazelcast.newHazelcastInstance(config)
//  }
//
//  private fun chatRoomsMapConfig(): MapConfig {
//    return MapConfig()
//      .setName(chatRoomsMap)
//      .setBackupCount(1)
//      .setAsyncBackupCount(2)
//  }
//
//  private fun chatRoomUsersListConfig(): ListConfig {
//    return ListConfig()
//      .setName(usersInChatRoomList)
//      .setBackupCount(1)
//      .setAsyncBackupCount(2)
//  }
//
//  fun getChatRoomsMap(): IMap<String, ChatRoom> {
//    return hzInstance.getMap<String, ChatRoom>(chatRoomsMap)
//  }
//
//  companion object {
//    const val chatRoomsMap = "chat_rooms_map"
//    const val usersInChatRoomList = "users_in_chat_room"
//  }
}