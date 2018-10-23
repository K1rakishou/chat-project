package model.user

import javafx.beans.property.SimpleStringProperty

abstract class BaseUserItem(
  userName: String
) {
  private val userNameProperty by lazy { SimpleStringProperty(userName) }
  fun userNameProperty() = userNameProperty
  var userName: String
    get() = userNameProperty.get()
    set(value) = userNameProperty.set(value)

  override fun equals(other: Any?): Boolean {
    if (other == null) {
      return false
    }

    if (other !is BaseUserItem) {
      return false
    }

    if (this === other) {
      return true
    }

    return userName == other.userName
  }

  override fun hashCode(): Int {
    return userName.hashCode()
  }
}