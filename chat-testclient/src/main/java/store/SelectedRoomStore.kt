package store

import javafx.beans.property.SimpleStringProperty

class SelectedRoomStore {
  private val prevSelectedRoomProperty = SimpleStringProperty(null)
  private val selectedRoomProperty = SimpleStringProperty(null)

  fun getSelectedRoomProperty(): SimpleStringProperty {
    return selectedRoomProperty
  }

  fun getSelectedRoom(): String? {
    return selectedRoomProperty.get()
  }

  fun getPrevSelectedRoom(): String? {
    return prevSelectedRoomProperty.get()
  }

  fun getSelectedOrPrevSelected(): String? {
    if (selectedRoomProperty.get() != null) {
      return getSelectedRoom()
    }

    return getPrevSelectedRoom()
  }

  fun setSelectedRoom(roomName: String): Boolean {
    if (roomName == selectedRoomProperty.get()) {
      return false
    }

    prevSelectedRoomProperty.set(selectedRoomProperty.get())
    selectedRoomProperty.set(roomName)
    return true
  }

  fun clearSelection() {
    selectedRoomProperty.set(null)
    prevSelectedRoomProperty.set(null)
  }
}