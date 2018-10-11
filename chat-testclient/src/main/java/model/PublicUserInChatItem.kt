package model

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.ItemViewModel
import java.util.*

class PublicUserInChatItem(
  userName: String
) {
  val id = UUID.randomUUID()

  private val userNameProperty by lazy { SimpleStringProperty(userName) }
  fun userNameProperty() = userNameProperty
  var userName: String
    get() = userNameProperty.get()
    set(value) = userNameProperty.set(value)

  override fun equals(other: Any?): Boolean {
    if (other == null) {
      return false
    }

    if (other !is PublicUserInChatItem) {
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

class PublicUserInChatItemModel(
  property: ObjectProperty<PublicUserInChatItem>
) : ItemViewModel<PublicUserInChatItem>(itemProperty = property) {
  val userName = bind(autocommit = true) { item?.userNameProperty() }
}