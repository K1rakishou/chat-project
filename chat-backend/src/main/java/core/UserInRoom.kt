package core

class UserInRoom(
  val user: User
) {
  override fun equals(other: Any?): Boolean {
    if (other == null) {
      return false
    }

    if (other !is UserInRoom) {
      return false
    }


    return user == other
  }

  override fun hashCode(): Int {
    return user.hashCode()
  }

  override fun toString(): String {
    return user.toString()
  }
}