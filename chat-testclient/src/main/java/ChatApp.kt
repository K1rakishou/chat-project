import javafx.stage.Stage
import manager.NetworkManager
import tornadofx.App
import ui.chat_room_list_window.ChatRoomListWindow

class ChatApp : App(ChatRoomListWindow::class, Styles::class) {
  val networkManager = NetworkManager().also { it.run() }

  override fun start(stage: Stage) {
    super.start(stage)

    stage.width = 384.0
    stage.height = 720.0
  }
}