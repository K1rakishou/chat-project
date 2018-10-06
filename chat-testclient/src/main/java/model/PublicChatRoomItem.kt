package model

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import model.chat_message.BaseChatMessageItem
import tornadofx.ItemViewModel
import java.util.*

class PublicChatRoomItem(
  roomName: String,
  usersCount: Short,
  userList: ObservableList<PublicUserInChatItem>,
  roomMessages: ObservableList<BaseChatMessageItem>
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

  private val userListProperty by lazy { SimpleListProperty<PublicUserInChatItem>(userList) }
  fun userListProperty() = userListProperty
  var userList: ObservableList<PublicUserInChatItem>
    get() = userListProperty.get()
    set(value) = userListProperty.set(value)

  private val roomMessagesProperty by lazy { SimpleListProperty<BaseChatMessageItem>(roomMessages) }
  fun roomMessagesProperty() = roomMessagesProperty
  var roomMessages: ObservableList<BaseChatMessageItem>
    get() = roomMessagesProperty.get()
    set(value) = roomMessagesProperty.set(value)

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
  val userList = bind(autocommit = true) { item?.userListProperty() }
  val roomMessages = bind(autocommit = true) { item?.roomMessagesProperty() }
}