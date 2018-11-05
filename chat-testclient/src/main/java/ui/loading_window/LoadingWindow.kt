package ui.loading_window

import controller.LoadingWindowController
import events.ConnectionWindowEvents
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.Alert
import javafx.scene.text.TextAlignment
import model.viewmodel.HostInfoViewModel
import tornadofx.*
import ui.base.BaseView
import ui.chat_main_window.ChatMainWindow
import ui.connection_window.ConnectionWindow

class LoadingWindow : BaseView("Chat") {
  private val loadingWindowController: LoadingWindowController by inject()
  private val hostInfoViewModel: HostInfoViewModel by inject()

  private val connectionStatus = SimpleStringProperty("")

  override fun onDock() {
    loadingWindowController.createController(this)

    if (hostInfoViewModel.isEmpty()) {
      find<LoadingWindow>().replaceWith<ConnectionWindow>()
      return
    }

    loadingWindowController.startConnectionToServer(hostInfoViewModel.ip, hostInfoViewModel.port)
  }

  override fun onUndock() {
    loadingWindowController.destroyController()
  }

  override val root = vbox(alignment = Pos.CENTER) {
    prefHeight = 200.0
    prefWidth = 300.0

    progressindicator {
    }
    label(observable = connectionStatus) {
      textAlignment = TextAlignment.CENTER
      wrapTextProperty().set(true)
    }

    paddingBottom = 32.0

    button("Cancel") {
      action {
        loadingWindowController.stopConnectionToServer()
      }
    }
  }

  fun updateConnectionStatus(message: String) {
    doOnUI {
      connectionStatus.set(message)
    }
  }

  fun showConnectionError(message: String) {
    doOnUI {
      alert(Alert.AlertType.ERROR, header = "Connection error", content = message)
    }
  }

  fun closeView() {
    doOnUI {
      close()
    }
  }

  fun onConnectedToServer() {
    doOnUI {
      find<ChatMainWindow>().openWindow(resizable = true, escapeClosesWindow = false)

      closeView()
      fire(ConnectionWindowEvents.CloseConnectionWindowEvent)
    }
  }

}