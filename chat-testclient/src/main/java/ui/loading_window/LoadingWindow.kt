package ui.loading_window

import controller.LoadingWindowController
import javafx.geometry.Pos
import javafx.scene.control.Alert
import javafx.scene.text.TextAlignment
import model.viewmodel.HostInfoViewModel
import tornadofx.*
import ui.connection_window.ConnectionWindow

class LoadingWindow : View("Chat") {
  private val loadingWindowController: LoadingWindowController by inject()
  private val hostInfoViewModel: HostInfoViewModel by inject()

  override fun onDock() {
    loadingWindowController.createController()

    if (hostInfoViewModel.isEmpty()) {
      find<LoadingWindow>().replaceWith<ConnectionWindow>()
      return
    }

    loadingWindowController.connectionError.addListener { _, _, newValue ->
      if (newValue == null) {
        return@addListener
      }

      alert(Alert.AlertType.ERROR, header = "Connection error", content = newValue)
    }

    loadingWindowController.startConnectionToServer(hostInfoViewModel.host, hostInfoViewModel.port)
  }

  override fun onUndock() {
    loadingWindowController.destroyController()
  }

  override val root = vbox(alignment = Pos.CENTER) {
    prefHeight = 200.0
    prefWidth = 300.0

    progressindicator {
    }
    label(observable = loadingWindowController.connectionStatus) {
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

}