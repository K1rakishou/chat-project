package model.links

abstract class AbstractLink(
  val url: String
) {

  override fun toString(): String {
    return "url = $url"
  }
}