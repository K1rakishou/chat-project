package core

import org.junit.Assert.assertEquals
import org.junit.Test

class LinksExtractorTest {

  @Test
  fun `test links extractor`() {
    val message =
      """
        I have a block of text, that can have zero, or many urls. I'm trying to get a list of images. There are no img tags, just a links.
        In:
        var text = "foo bar https://i.imgur.com/WTPqq.jpg , bar foo http://i.imgur.com/uqaWNCR.jpg";
        Out:
        var urls = ["http://i.imgur.com/WTPqq.gif", "http://i.imgur.com/uqaWNCR.jpg" ];
      """

    val links = LinksExtractor.extract(message)

    assertEquals(3, links.size)
  }
}