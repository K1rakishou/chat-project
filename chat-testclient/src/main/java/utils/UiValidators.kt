package utils

import core.Constants
import tornadofx.ValidationContext
import tornadofx.ValidationMessage

object UiValidators {

  fun validateUserName(context: ValidationContext, userName: String?): ValidationMessage? {
    if (userName == null) {
      return null
    }

    if (userName.isBlank()) {
      return context.error("User name cannot consist solely from whitespaces")
    }

    if (userName.length < Constants.minUserNameLen) {
      return context.error("User name should be at least ${Constants.minUserNameLen} symbols")
    }

    if (userName.length > Constants.maxUserNameLen) {
      return context.error("User name must not exceed ${Constants.maxUserNameLen} symbols")
    }

    return null
  }

  fun validateRoomImageUrl(context: ValidationContext, roomImageUrl: String?): ValidationMessage? {
    try {
      if (roomImageUrl.isNullOrBlank()) {
        return context.error("Room image url cannot be empty or blank")
      }

      val split1 = roomImageUrl.split("//")
      if (split1[0] != "https:") {
        return context.error("Room image url should start with \"https\"")
      }

      val split2 = split1[1].split("/")
      if (!split2[0].startsWith("i.imgur.com")) {
        return context.error("Not an \"i.imgur.com\" url")
      }

      val split3 = split2[1].split('.')
      if (split3[1] != "jpg" && split3[1] != "png" && split3[1] != "jpeg") {
        return context.error("Image should be either JPG/JPEG or PNG")
      }
    } catch (error: Throwable) {
      return context.error("Parsing error: ${error.message}")
    }

    return null
  }

  fun validateRoomPassword(context: ValidationContext, roomPassword: String?): ValidationMessage? {
    if (roomPassword == null) {
      return null
    }

    if (roomPassword.isBlank()) {
      return context.error("Room password cannot consist solely from whitespaces")
    }

    if (roomPassword.length < Constants.minChatRoomPasswordLen) {
      return context.error("Room password should be at least ${Constants.minChatRoomPasswordLen} symbols")
    }

    if (roomPassword.length > Constants.maxChatRoomPasswordLen) {
      return context.error("Room password must not exceed ${Constants.maxChatRoomPasswordLen} symbols")
    }

    return null
  }

  fun validateRoomName(context: ValidationContext, roomName: String?): ValidationMessage? {
    if (roomName.isNullOrBlank()) {
      return context.error("Room name cannot be empty or blank")
    }

    if (roomName.length < Constants.minChatRoomNameLen) {
      return context.error("Room name should be at least ${Constants.minChatRoomNameLen} symbols")
    }

    if (roomName.length > Constants.maxChatRoomNameLength) {
      return context.error("Room name must not exceed ${Constants.maxChatRoomNameLength} symbols")
    }

    return null
  }

}