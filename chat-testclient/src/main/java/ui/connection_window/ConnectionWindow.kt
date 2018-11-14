package ui.connection_window

import ChatApp.Companion.settingsStore
import events.ConnectionWindowEvents
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import model.viewmodel.HostInfoViewModel
import store.settings.ConnectionWindowSettings
import store.settings.SharedSettings
import tornadofx.*
import ui.base.BaseView
import ui.loading_window.LoadingWindow
import utils.UiValidators

class ConnectionWindow : BaseView("Connection parameters") {
  private val connectionWindowSettings: ConnectionWindowSettings by lazy { ChatApp.settingsStore.connectionWindowSettings }
  private val sharedSettings: SharedSettings by lazy { ChatApp.settingsStore.sharedSettings }

  private val model = object : ViewModel() {
    val ip = bind { SimpleStringProperty(ConnectionWindowSettings.ipAddressDefault) }
    val port = bind { SimpleStringProperty(ConnectionWindowSettings.portDefault) }
    val userName = bind { SimpleStringProperty(SharedSettings.userNameDefault) }
  }

  init {
    settingsStore.read()

    model.ip.value = connectionWindowSettings.ipAddress
    model.port.value = connectionWindowSettings.port
    model.userName.value = sharedSettings.userName

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
    addClass(Styles.connectionWindow)

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
      field("User Name") {
        textfield(model.userName) {
          validator { userName ->
            if (userName?.isEmpty() == true) {
              model.userName.value = null
              UiValidators.validateUserName(this, model.userName.value)
            } else {
              UiValidators.validateUserName(this, userName)
            }
          }

          tooltip("If you don't want to enter your user name every time you join a chat room \n you can just do it once here")
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
        addClass(Styles.connectButton)

        isDefaultButton = true
        enableWhen { model.valid }

        action {
          model.commit {
            connectionWindowSettings.updateIpAddress(model.ip.value)
            connectionWindowSettings.updatePort(model.port.value)
            sharedSettings.updateUserName(model.userName.value)

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