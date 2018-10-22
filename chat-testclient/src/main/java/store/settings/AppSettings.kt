package store.settings

import java.lang.RuntimeException
import java.lang.StringBuilder
import java.util.concurrent.atomic.AtomicInteger

abstract class AppSettings {
  abstract fun read(settingLines: List<String>, settingIndex: AtomicInteger)
  abstract fun store(sb: StringBuilder)

  protected fun <T> readValue(settingName: String, setting: String, converter: (String) -> T): T {
    val (name, value) = setting.split("=")
    if (name != settingName) {
      throw RuntimeException("Unknown setting (${name}) should be (${settingName})")
    }

    return converter(value)
  }
}