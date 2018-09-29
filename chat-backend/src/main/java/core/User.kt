package core

class User(
  val userName: String,
  val ecPublicKey: ByteArray
) {

  override fun equals(other: Any?): Boolean {
    if (other == null) {
      return false
    }

    if (other !is User) {
      return false
    }

    return other.userName == this.userName
  }

  override fun hashCode(): Int {
    return 31 * userName.hashCode()
  }

  override fun toString(): String {
    return "[$userName]"
  }
}