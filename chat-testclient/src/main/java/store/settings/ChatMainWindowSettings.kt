package store.settings

import java.lang.StringBuilder
import java.util.concurrent.atomic.AtomicInteger

class ChatMainWindowSettings : AppSettings() {
  private val settingWindowXposition = "windowXposition"
  var windowXposition: Double = windowXpositionDefault
    private set
  fun updateWindowXposition(value: Double?) {
    windowXposition = value ?: windowXpositionDefault
  }

  private val settingWindowYposition = "windowYposition"
  var windowYposition: Double = windowYpositionDefault
    private set
  fun updateWindowYposition(value: Double?) {
    windowYposition = value ?: windowYpositionDefault
  }

  private val settingWindowWidth = "windowWidth"
  var windowWidth: Double = windowWidthDefault
    private set
  fun updateWindowWidth(value: Double?) {
    windowWidth = value ?: windowWidthDefault
  }

  private val settingWindowHeight = "windowHeight"
  var windowHeight: Double = windowHeightDefault
    private set
  fun updateWindowHeight(value: Double?) {
    windowHeight = value ?: windowHeightDefault
  }

  override fun read(settingLines: List<String>, settingIndex: AtomicInteger) {
    val windowXpositionValue = getDoubleValue(settingWindowXposition, settingLines[settingIndex.getAndIncrement()])
    val windowYpositionValue = getDoubleValue(settingWindowYposition, settingLines[settingIndex.getAndIncrement()])
    val windowWidthValue = getDoubleValue(settingWindowWidth, settingLines[settingIndex.getAndIncrement()])
    val windowHeightValue = getDoubleValue(settingWindowHeight, settingLines[settingIndex.getAndIncrement()])

    windowXposition = windowXpositionValue
    windowYposition = windowYpositionValue
    windowWidth = windowWidthValue
    windowHeight = windowHeightValue
  }

  override fun store(sb: StringBuilder) {
    sb.append("$settingWindowXposition=$windowXposition\n")
    sb.append("$settingWindowYposition=$windowYposition\n")
    sb.append("$settingWindowWidth=$windowWidth\n")
    sb.append("$settingWindowHeight=$windowHeight\n")
  }

  companion object {
    const val windowXpositionDefault = 100.0
    const val windowYpositionDefault = 100.0
    const val windowWidthDefault = 720.0
    const val windowHeightDefault = 400.0
  }
}