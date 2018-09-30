package model

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.ItemViewModel
import java.util.*

class PublicChatRoomItem(
  roomName: String,
  usersCount: Short
) {
  val id = UUID.randomUUID()

  private val roomNameProperty by lazy { SimpleStringProperty(roomName) }
  fun roomNameProperty() = roomNameProperty
  var roomName: String
    get() = roomNameProperty.get()
    set(value) = roomNameProperty.set(value)

  private val usersCountProperty by lazy { SimpleIntegerProperty(usersCount.toInt()) }
  fun usersCountProperty() = usersCountProperty
  var usersCount: Int
    get() = usersCountProperty.get()
    set(value) = usersCountProperty.set(value)

  override fun equals(other: Any?): Boolean {
    if (other == null) {
      return false
    }

    if (other !is PublicChatRoomItem) {
      return false
    }

    if (this === other) {
      return true
    }

    return id == other.id
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }
}

class PublicChatRoomItemModel(
  property: ObjectProperty<PublicChatRoomItem>
) : ItemViewModel<PublicChatRoomItem>(itemProperty = property) {
  val roomName = bind(autocommit = true) { item?.roomNameProperty() }
  val usersCount = bind(autocommit = true) { item?.usersCountProperty() }
}