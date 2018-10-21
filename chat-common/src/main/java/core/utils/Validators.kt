package core.utils

import core.Constants
import core.Status

object Validators {

  fun isImageUrlValid(url: String): Boolean {
    try {
      //for now only allow images from imgur.com
      //https://i.imgur.com/xxx.jpg

      if (url.isBlank()) {
        return false
      }

      val split1 = url.split("//")
      if (split1[0] != "https:") {
        return false
      }

      val split2 = split1[1].split("/")
      if (!split2[0].startsWith("i.imgur.com")) {
        return false
      }

      val split3 = split2[1].split('.')
      return split3[1] == "jpg" || split3[1] == "png" || split3[1] == "jpeg"
    } catch (error: Throwable) {
      error.printStackTrace()
      return false
    }
  }

  fun validateChatRoomName(chatRoomName: String): Status? {
    if (chatRoomName.isBlank()) {
      println("chatRoomName is blank: ${chatRoomName}")
      return Status.BadParam
    }

    if (chatRoomName.length < Constants.minChatRoomNameLen) {
      println("chatRoomName.length (${chatRoomName.length}) < Constants.minChatRoomNameLen (${Constants.minChatRoomNameLen})")
      return Status.BadParam
    }

    if (chatRoomName.length > Constants.maxChatRoomNameLength) {
      println("chatRoomName.length (${chatRoomName.length}) > Constants.maxChatRoomNameLength (${Constants.maxChatRoomNameLength})")
      return Status.BadParam
    }

    if (chatRoomName.isBlank()) {
      println("chatRoomName is blank ($chatRoomName)")
      return Status.BadParam
    }

    return null
  }

  fun validateChatRoomPasswordHash(chatRoomPasswordHash: String?): Status? {
    chatRoomPasswordHash?.let { roomPasswordHash ->
      if (roomPasswordHash.isBlank()) {
        println("chatRoomPasswordHash is blank ($roomPasswordHash)")
        return Status.BadParam
      }

      if (roomPasswordHash.length < Constants.minChatRoomPasswordLen) {
        println("chatRoomPasswordHash (${roomPasswordHash.length}) < Constants.minChatRoomPasswordLen (${Constants.minChatRoomPasswordLen})")
        return Status.BadParam
      }

      if (roomPasswordHash.length > Constants.maxChatRoomPasswordHashLen) {
        println("chatRoomPasswordHash (${roomPasswordHash.length}) > Constants.minChatRoomPasswordLen (${Constants.maxChatRoomPasswordHashLen})")
        return Status.BadParam
      }
    }

    return null
  }

  fun validateUserName(userName: String?): Status? {
    userName?.let { name ->
      if (name.isBlank()) {
        println("userName is blank ($name)")
        return Status.BadParam
      }

      if (name.length < Constants.minUserNameLen) {
        println("userName.length (${name.length}) < Constants.minUserNameLen (${Constants.minUserNameLen})")
        return Status.BadParam
      }

      if (name.length > Constants.maxUserNameLen) {
        println("userName.length (${name.length}) > Constants.minUserNameLen (${Constants.minUserNameLen})")
        return Status.BadParam
      }
    }

    return null
  }
}