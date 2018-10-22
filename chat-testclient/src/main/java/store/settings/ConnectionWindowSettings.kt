package store.settings

import java.lang.StringBuilder
import java.util.concurrent.atomic.AtomicInteger

class ConnectionWindowSettings : AppSettings() {
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

  private val settingIpAddress = "ipAddress"
  var ipAddress: String = ipAddressDefault
    private set
  fun updateIpAddress(value: String?) {
    ipAddress = value ?: ipAddressDefault
  }

  private val settingPort = "port"
  var port: String = portDefault
    private set
  fun updatePort(value: String?) {
    port = value ?: portDefault
  }

  override fun read(settingLines: List<String>, settingIndex: AtomicInteger) {
    val windowXpositionValue = readValue(settingWindowXposition, settingLines[settingIndex.getAndIncrement()]) { it.toDouble() }
    val windowYpositionValue = readValue(settingWindowYposition, settingLines[settingIndex.getAndIncrement()]) { it.toDouble() }
    val ipAddressValue = readValue(settingIpAddress, settingLines[settingIndex.getAndIncrement()]) { it }
    val portValue = readValue(settingPort, settingLines[settingIndex.getAndIncrement()]) { it }

    windowXposition = windowXpositionValue
    windowYposition = windowYpositionValue
    ipAddress = ipAddressValue
    port = portValue
  }

  override fun store(sb: StringBuilder) {
    sb.append("$settingWindowXposition=$windowXposition\n")
    sb.append("$settingWindowYposition=$windowYposition\n")
    sb.append("$settingIpAddress=$ipAddress\n")
    sb.append("$settingPort=$port\n")
  }

  companion object {
    const val windowXpositionDefault = 100.0
    const val windowYpositionDefault = 100.0
    const val ipAddressDefault = "127.0.0.1"
    const val portDefault = "2323"

  }
}