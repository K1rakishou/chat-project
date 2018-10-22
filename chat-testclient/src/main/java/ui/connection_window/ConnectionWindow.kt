package ui.connection_window

import ChatApp.Companion.settingsStore
import events.ConnectionWindowEvents
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import model.viewmodel.HostInfoViewModel
import store.settings.ConnectionWindowSettings
import tornadofx.*
import ui.base.BaseView
import ui.loading_window.LoadingWindow
import utils.UiValidators

class ConnectionWindow : BaseView("Connection parameters") {
  private val connectionWindowSettings: ConnectionWindowSettings by lazy { settingsStore.connectionWindowSettings }

  private val model = object : ViewModel() {
    val ip = bind { SimpleStringProperty(ConnectionWindowSettings.ipAddressDefault) }
    val port = bind { SimpleStringProperty(ConnectionWindowSettings.portDefault) }
  }

  init {
    settingsStore.read()

    model.ip.value = connectionWindowSettings.ipAddress
    model.port.value = connectionWindowSettings.port

    subscribe<ConnectionWindowEvents.CloseConnectionWindowEvent> {
      close()
    }.autoUnsubscribe()
  }

  override fun onDock() {
    currentWindow?.x = connectionWindowSettings.windowXposition
    currentWindow?.y = connectionWindowSettings.windowYposition
  }

  override fun onUndock() {
    connectionWindowSettings.updateWindowXposition(currentWindow?.x)
    connectionWindowSettings.updateWindowYposition(currentWindow?.y)

    settingsStore.save()
  }

  override val root = form {
    prefHeight = 150.0
    prefWidth = 350.0
    paddingAll = 10.0

    fieldset {
      field("IP Address") {
        textfield(model.ip) {
          validator { host ->
            UiValidators.validateIP(this, host)
          }

          whenDocked { requestFocus() }
        }
      }
      field("Port") {
        textfield(model.port) {
          validator { port ->
            UiValidators.validatePortNumber(this, port)
          }
        }
      }
    }
    label {
      prefHeight = 8.0
    }
    hbox {
      hboxConstraints { hgrow = Priority.ALWAYS }
      alignment = Pos.BASELINE_RIGHT

      button("Connect") {
        isDefaultButton = true
        enableWhen { model.valid }

        action {
          model.commit {
            connectionWindowSettings.updateIpAddress(model.ip.value)
            connectionWindowSettings.updatePort(model.port.value)

            showConnectionToServerWindow()
          }
        }
      }
    }
  }

  private fun showConnectionToServerWindow() {
    val newScope = Scope()
    setInScope(HostInfoViewModel(model.ip.value, model.port.value), newScope)

    find<LoadingWindow>(newScope).openModal(resizable = false)
  }

}