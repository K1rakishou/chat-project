package store

import tornadofx.Controller
import java.io.File
import java.lang.IllegalStateException
import java.lang.StringBuilder
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

class SettingsStore : Controller() {
  private val settingsFile = File(System.getProperty("user.dir") + "\\settings.dat")
  val chatMainWindowSettings = ChatMainWindowSettings()

  fun init() {
    readSettings()
  }

  fun close() {
    storeSettings()
  }

  private fun createDefault() {
    if (!settingsFile.exists()) {
      settingsFile.createNewFile()

      //create a new file with default settings
      storeSettings()
    }
  }

  private fun readSettings() {
    createDefault()

    val lines = settingsFile.readLines()
    if (lines.size <= 1) {
      deleteSettingsFile()
      createDefault()
      return
    }

    //first line is a warning and not the actual setting
    val settingLines = lines.drop(1)
    val settingIndex = AtomicInteger(0)

    try {
      chatMainWindowSettings.read(settingLines, settingIndex)
    } catch (error: Throwable) {
      error.printStackTrace()
    }
  }

  private fun storeSettings() {
    try {
      val string = buildString(128) {
        append("===DO NOT CHANGE THIS FILE!===\n")
        chatMainWindowSettings.store(this)
      }

      settingsFile.writeText(string)
    } catch (error: Throwable) {
      error.printStackTrace()
      deleteSettingsFile()
    }
  }

  private fun deleteSettingsFile() {
    Files.deleteIfExists(settingsFile.toPath())
  }

  class ChatMainWindowSettings : AppSettings() {
    private val settingChatMainWindowXposition = "chatMainWindowXposition"
    var chatMainWindowXposition: Double = chatMainWindowXpositionDefault
      private set
    fun setChatMainWindowXposition(value: Double?) {
      chatMainWindowXposition = value ?: chatMainWindowXpositionDefault
    }

    private val settingChatMainWindowYposition = "chatMainWindowYposition"
    var chatMainWindowYposition: Double = chatMainWindowYpositionDefault
      private set
    fun setChatMainWindowYposition(value: Double?) {
      chatMainWindowYposition = value ?: chatMainWindowYpositionDefault
    }

    private val settingChatMainWindowWidth = "chatMainWindowWidth"
    var chatMainWindowWidth: Double = chatMainWindowWidthDefault
      private set
    fun setChatMainWindowWidth(value: Double?) {
      chatMainWindowWidth = value ?: chatMainWindowWidthDefault
    }

    private val settingChatMainWindowHeight = "chatMainWindowHeight"
    var chatMainWindowHeight: Double = chatMainWindowHeightDefault
      private set
    fun setChatMainWindowHeight(value: Double?) {
      chatMainWindowHeight = value ?: chatMainWindowHeightDefault
    }

    override fun read(settingLines: List<String>, settingIndex: AtomicInteger) {
      val chatMainWindowXpositionValue = getDoubleValue(settingChatMainWindowXposition, settingLines[settingIndex.getAndIncrement()])
      val settingChatMainWindowYpositionValue = getDoubleValue(settingChatMainWindowYposition, settingLines[settingIndex.getAndIncrement()])
      val settingChatMainWindowWidthValue = getDoubleValue(settingChatMainWindowWidth, settingLines[settingIndex.getAndIncrement()])
      val settingChatMainWindowHeightValue = getDoubleValue(settingChatMainWindowHeight, settingLines[settingIndex.getAndIncrement()])

      chatMainWindowXposition = chatMainWindowXpositionValue
      chatMainWindowYposition = settingChatMainWindowYpositionValue
      chatMainWindowWidth = settingChatMainWindowWidthValue
      chatMainWindowHeight = settingChatMainWindowHeightValue
    }

    override fun store(sb: StringBuilder) {
      sb.append("$settingChatMainWindowXposition=$chatMainWindowXposition\n")
      sb.append("$settingChatMainWindowYposition=$chatMainWindowYposition\n")
      sb.append("$settingChatMainWindowWidth=$chatMainWindowWidth\n")
      sb.append("$settingChatMainWindowHeight=$chatMainWindowHeight\n")
    }

    companion object {
      const val chatMainWindowXpositionDefault = 100.0
      const val chatMainWindowYpositionDefault = 100.0
      const val chatMainWindowWidthDefault = 720.0
      const val chatMainWindowHeightDefault = 400.0
    }
  }

  abstract class AppSettings {
    abstract fun read(settingLines: List<String>, settingIndex: AtomicInteger)
    abstract fun store(sb: StringBuilder)

    protected fun getDoubleValue(settingName: String, setting: String): Double {
      val (name, value) = setting.split("=")
      if (name != settingName) {
        throw IllegalStateException("Unknown setting (${name}) should be (${settingName})")
      }

      return value.toDouble()
    }
  }
}