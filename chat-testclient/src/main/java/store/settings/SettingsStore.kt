package store.settings

import tornadofx.Controller
import java.io.File
import java.lang.RuntimeException
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

class SettingsStore : Controller() {
  private val settingsFile = File(System.getProperty("user.dir") + "\\settings.dat")
  private var readSettingsFile = false
  val chatMainWindowSettings = ChatMainWindowSettings()
  val connectionWindowSettings = ConnectionWindowSettings()
  val sharedSettings = SharedSettings()

  fun read() {
    if (readSettingsFile) {
      throw RuntimeException("Must not read settings file more than once!")
    }

    readSettings()
    readSettingsFile = true
  }

  fun save() {
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

    val settingLines = settingsFile.readLines()
    if (settingLines.size <= 1) {
      deleteSettingsFile()
      createDefault()
      return
    }

    val settingIndex = AtomicInteger(0)

    try {
      settingIndex.incrementAndGet() //skip "WARNING"

      settingIndex.incrementAndGet() //skip "Shared Settings"
      sharedSettings.read(settingLines, settingIndex)

      settingIndex.incrementAndGet() //skip "ChatMainWindow Settings"
      chatMainWindowSettings.read(settingLines, settingIndex)

      settingIndex.incrementAndGet() //skip "ConnectionWindow Settings"
      connectionWindowSettings.read(settingLines, settingIndex)
    } catch (error: Throwable) {
      error.printStackTrace()
    }
  }

  private fun storeSettings() {
    try {
      val string = buildString(512) {
        append("//WARNING!!! CHANGE THIS FILE ONLY IF YOU UNDERSTAND WHAT YOU ARE DOING. IF SOMETHING GOES WRONG - JUST DELETE THIS FILE\n")

        append("//Shared Settings\n")
        sharedSettings.store(this)

        append("//ChatMainWindow Settings\n")
        chatMainWindowSettings.store(this)

        append("//ConnectionWindow Settings\n")
        connectionWindowSettings.store(this)
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

}