import manager.NetworkManager
import tornadofx.App
import ui.loading_window.ConnectionToServerWindow

class ChatApp : App(ConnectionToServerWindow::class, Styles::class) {
  val networkManager = NetworkManager()
}