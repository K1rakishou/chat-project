import javafx.stage.Stage
import manager.NetworkManager
import store.ChatRoomsStore
import store.SearchChatRoomsStore
import store.SelectedRoomStore
import store.SettingsStore
import tornadofx.App
import tornadofx.NoPrimaryViewSpecified
import tornadofx.find
import ui.connection_window.ConnectionWindow

class ChatApp : App(NoPrimaryViewSpecified::class, Styles::class) {

  override fun start(stage: Stage) {
    super.start(stage)

    find<ConnectionWindow>().openWindow(resizable = false)
  }

  companion object {
    //singleton dependencies
    val networkManager = NetworkManager()
    val settingsStore = SettingsStore()
    val chatRoomsStore = ChatRoomsStore()
    val searchChatRoomsStore = SearchChatRoomsStore()
    val selectedRoomStore = SelectedRoomStore()
  }
}