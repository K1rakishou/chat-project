package store

import javafx.beans.property.SimpleStringProperty

class SelectedRoomStore {
  private val selectedRoomProperty = SimpleStringProperty()

  fun getSelectedRoom() = selectedRoomProperty
  fun clearSelectedRoom() = selectedRoomProperty.set(null)

  fun setSelectedRoom(roomName: String): Boolean {
    if (roomName == selectedRoomProperty.get()) {
      return false
    }

    selectedRoomProperty.set(roomName)
    return true
  }
}