package ui.loading_window

import controller.ConnectionToServerController
import javafx.geometry.Pos
import javafx.scene.control.Alert
import javafx.scene.text.TextAlignment
import model.viewmodel.HostInfoViewModel
import tornadofx.*
import ui.connection_window.ConnectionWindow

class ConnectionToServerWindow : View("Chat") {
  private val connectionToServerController: ConnectionToServerController by inject()
  private val hostInfoViewModel: HostInfoViewModel by inject()

  override fun onDock() {
    connectionToServerController.createController()

    if (hostInfoViewModel.isEmpty()) {
      find<ConnectionToServerWindow>().replaceWith<ConnectionWindow>()
      return
    }

    connectionToServerController.connectionError.addListener { _, _, newValue ->
      if (newValue == null) {
        return@addListener
      }

      alert(Alert.AlertType.ERROR, header = "Connection error", content = newValue)
      connectionToServerController.goBackToConnectionWindow()
    }

    connectionToServerController.startConnectionToServer(hostInfoViewModel.host, hostInfoViewModel.port)
  }

  override fun onUndock() {
    connectionToServerController.destroyController()
  }

  override val root = vbox(alignment = Pos.CENTER) {
    prefHeight = 200.0
    prefWidth = 300.0

    progressindicator {
    }
    label(observable = connectionToServerController.connectionStatus) {
      textAlignment = TextAlignment.CENTER
      wrapTextProperty().set(true)
    }

    paddingBottom = 32.0

    button("Cancel") {
      action {
        connectionToServerController.stopConnectionToServer()
      }
    }
  }

}