package controller

import ChatApp
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import javafx.beans.property.SimpleStringProperty
import manager.NetworkManager
import store.Store
import tornadofx.runLater
import ui.chat_main_window.ChatMainWindow
import ui.events.CloseConnectionWindowEvent
import ui.loading_window.LoadingWindow

class LoadingWindowController : BaseController() {
  private val networkManager = ChatApp.networkManager
  val store: Store by inject()
  val connectionStatus = SimpleStringProperty("")
  val connectionError = SimpleStringProperty(null)

  override fun createController() {
    super.createController()

    connectionError.set(null)
    connectionStatus.set("")
  }

  override fun destroyController() {
    super.destroyController()
  }

  fun startConnectionToServer(host: String, port: String) {
    changeConnectionStatus("Connecting...")
    networkManager.connect(host, port.toInt())

    startListeningToPackets()
  }

  fun stopConnectionToServer() {
    networkManager.disconnect()

    goBackToConnectionWindow()
  }

  private fun startListeningToPackets() {
    compositeDisposable += networkManager.connectionStateObservable
      .subscribeBy(onNext = { connectionState ->
        when (connectionState) {
          is NetworkManager.ConnectionState.Connecting -> {
            println("Connecting")
          }
          is NetworkManager.ConnectionState.Disconnected -> {
            goBackToConnectionWindow()
          }
          is NetworkManager.ConnectionState.ErrorWhileTryingToConnect -> {
            val additionalInfo = connectionState.error ?: "no error message"
            connectionError("Error while trying to connect. Additional error message: \n\n$additionalInfo")

            goBackToConnectionWindow()
          }
          is NetworkManager.ConnectionState.Connected -> {
            changeConnectionStatus("Connected")
            goToChatMainWindow()
          }
          is NetworkManager.ConnectionState.Uninitialized -> {
            //Default state
          }
        }
      })
  }

  private fun changeConnectionStatus(content: String?) {
    runLater {
      connectionStatus.set(content ?: "Unknown error")
    }
  }

  private fun connectionError(message: String) {
    runLater {
      connectionError.set(message)
    }
  }

  private fun goBackToConnectionWindow() {
    runLater {
      find<LoadingWindow>().close()
    }
  }

  private fun goToChatMainWindow() {
    runLater {
      find<ChatMainWindow>().openWindow(resizable = true)

      find<LoadingWindow>().close()
      fire(CloseConnectionWindowEvent)
    }
  }
}