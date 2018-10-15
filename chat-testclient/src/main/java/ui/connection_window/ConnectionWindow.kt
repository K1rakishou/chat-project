package ui.connection_window

import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import model.viewmodel.HostInfoViewModel
import tornadofx.*
import ui.events.CloseConnectionWindowEvent
import ui.loading_window.LoadingWindow

class ConnectionWindow : View("Connection parameters") {

  private val model = object : ViewModel() {
    val host = bind { SimpleStringProperty() }
    val port = bind { SimpleStringProperty() }
  }

  init {
    subscribe<CloseConnectionWindowEvent> {
      runLater {
        close()
      }
    }
  }

  override val root = form {
    prefHeight = 200.0
    prefWidth = 350.0
    paddingAll = 10.0

    fieldset {
      field("Host address") {
        textfield(model.host) {
          text = "127.0.0.1"
          required()
          whenDocked { requestFocus() }
        }
      }
      field("Port") {
        textfield(model.port) {
          text = "2323"
          required()
        }
      }
    }

    paddingBottom = 32.0

    button("Connect") {
      alignment = Pos.CENTER
      isDefaultButton = true

      enableWhen { model.valid }

      action {
        model.commit {
          //TODO: validate
          showConnectionToServerWindow()
        }
      }
    }
  }

  private fun showConnectionToServerWindow() {
    val newScope = Scope()
    setInScope(HostInfoViewModel(model.host.value, model.port.value), newScope)

    find<LoadingWindow>(newScope).openModal(resizable = false)
  }

}