package core

import model.links.AbstractLink
import model.links.ImageLink

object LinksExtractor {
  private val imgurImageLinkRegex = "(https?://i\\.imgur\\.com/(?:\\w{5,10}\\.(?:jpe?g|png)))".toRegex()

  fun extract(message: String): List<AbstractLink> {
    val links = imgurImageLinkRegex.findAll(message)
      .map { it.value }
      .map { ImageLink(it) }
      .toList()

    return links
  }
}