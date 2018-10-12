import manager.NetworkManager
import tornadofx.App
import ui.connection_window.ConnectionWindow
import ui.loading_window.ConnectionToServerWindow

class ChatApp : App(ConnectionWindow::class, Styles::class) {

  companion object {
    val networkManager = NetworkManager()
  }
}