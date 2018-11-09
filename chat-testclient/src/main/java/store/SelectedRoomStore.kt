package store

import javafx.beans.property.SimpleStringProperty

class SelectedRoomStore {
  private val selectedRoomProperty = SimpleStringProperty(null)

  fun getSelectedRoomProperty(): SimpleStringProperty {
    return selectedRoomProperty
  }

  fun getSelectedRoom(): String? {
    return selectedRoomProperty.get()
  }

  fun setSelectedRoom(roomName: String?): Boolean {
    if (roomName == selectedRoomProperty.get()) {
      return false
    }

    selectedRoomProperty.set(roomName)
    return true
  }
}