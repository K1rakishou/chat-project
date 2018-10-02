package ui.loading_window

import controller.ConnectionToServerController
import javafx.geometry.Pos
import javafx.scene.text.TextAlignment
import tornadofx.*

class ConnectionToServerWindow : View() {
  private val connectionToServerController: ConnectionToServerController by inject()

  override fun onDock() {
    connectionToServerController.startConnectionToServer()
  }

  override val root = vbox(alignment = Pos.CENTER) {
    setPrefSize(360.0, 480.0)

    progressindicator {
      vboxConstraints {
        marginBottom = 10.0
      }
    }
    label(observable = connectionToServerController.connectionStatus) {
      textAlignment = TextAlignment.CENTER
      wrapTextProperty().set(true)
    }
  }

}