import javafx.stage.Stage
import manager.NetworkManager
import tornadofx.App
import tornadofx.NoPrimaryViewSpecified
import tornadofx.find
import ui.connection_window.ConnectionWindow

class ChatApp : App(NoPrimaryViewSpecified::class, Styles::class) {

  override fun start(stage: Stage) {
    super.start(stage)

    find<ConnectionWindow>().openModal(resizable = false)
  }

  companion object {
    //singleton dependencies
    val networkManager = NetworkManager()
  }
}