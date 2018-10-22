package store.settings

import java.lang.StringBuilder
import java.util.concurrent.atomic.AtomicInteger

class SharedSettings : AppSettings() {
  private val settingUserName = "userName"
  var userName: String = userNameDefault
    private set
  fun updateUserName(value: String?) {
    userName = value ?: userNameDefault
  }

  override fun read(settingLines: List<String>, settingIndex: AtomicInteger) {
    val userNameValue = readValue(settingUserName, settingLines[settingIndex.getAndIncrement()]) { it }

    userName = userNameValue
  }

  override fun store(sb: StringBuilder) {
    sb.append("$settingUserName=$userName\n")
  }

  companion object {
    const val userNameDefault = ""
  }
}