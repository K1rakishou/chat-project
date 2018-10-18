package controller

import ChatApp
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import manager.NetworkManager
import store.Store
import ui.loading_window.LoadingWindow

class LoadingWindowController : BaseController<LoadingWindow>() {
  private val networkManager = ChatApp.networkManager
  val store: Store by inject()

  override fun createController(viewParam: LoadingWindow) {
    super.createController(viewParam)
  }

  override fun destroyController() {
    super.destroyController()
  }

  fun startConnectionToServer(host: String, port: String) {
    networkManager.doConnect(host, port.toInt())

    startListeningToPackets()
  }

  fun stopConnectionToServer() {
    networkManager.doDisconnect()

    view.closeView()
  }

  private fun startListeningToPackets() {
    compositeDisposable += networkManager.connectionStateObservable
      .subscribeBy(onNext = { connectionState ->
        when (connectionState) {
          is NetworkManager.ConnectionState.Uninitialized -> {
            view.updateConnectionStatus("Initiating the connection...")
          }
          is NetworkManager.ConnectionState.Connecting -> {
            view.updateConnectionStatus("Connecting...")
          }
          is NetworkManager.ConnectionState.Disconnected -> {
            view.closeView()
          }
          is NetworkManager.ConnectionState.ErrorWhileTryingToConnect -> {
            val additionalInfo = connectionState.error ?: "no error message"

            view.showConnectionError("Error while trying to connect. Additional error message: \n\n$additionalInfo")
            view.closeView()
          }
          is NetworkManager.ConnectionState.Connected -> {
            view.updateConnectionStatus("Connected")
            view.onConnectedToServer()
          }
          is NetworkManager.ConnectionState.Reconnected -> {
            throw IllegalStateException("Should not happen here")
          }
        }
      })
  }
}