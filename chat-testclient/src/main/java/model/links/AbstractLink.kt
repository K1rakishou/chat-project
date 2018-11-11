package model.links

abstract class AbstractLink(
  val value: String
) {

  override fun toString(): String {
    return "[value = $value]"
  }
}