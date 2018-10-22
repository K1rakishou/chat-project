package store.settings

import java.lang.RuntimeException
import java.lang.StringBuilder
import java.util.concurrent.atomic.AtomicInteger

abstract class AppSettings {
  abstract fun read(settingLines: List<String>, settingIndex: AtomicInteger)
  abstract fun store(sb: StringBuilder)

  protected fun getDoubleValue(settingName: String, setting: String): Double {
    val (name, value) = setting.split("=")
    if (name != settingName) {
      throw RuntimeException("Unknown setting (${name}) should be (${settingName})")
    }

    return value.toDouble()
  }

  protected fun getStringValue(settingName: String, setting: String): String {
    val (name, value) = setting.split("=")
    if (name != settingName) {
      throw RuntimeException("Unknown setting (${name}) should be (${settingName})")
    }

    return value
  }

  protected fun getIntValue(settingName: String, setting: String): Int {
    val (name, value) = setting.split("=")
    if (name != settingName) {
      throw RuntimeException("Unknown setting (${name}) should be (${settingName})")
    }

    return value.toInt()
  }
}