package core.utils

object Validators {

  fun isImageUrlValid(url: String): Boolean {
    try {
      //for now only allow images from imgur.com
      //https://i.imgur.com/xxx.jpg

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
}