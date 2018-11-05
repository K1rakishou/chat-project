package core

import core.model.drainable.ChatRoomData
import core.model.drainable.PublicUserInChat
import core.model.drainable.chat_message.TextChatMessage
import org.junit.Assert.*
import org.junit.Test

class SizeofKtTest {

  @Test
  fun `sizeof Byte`() {
    assertEquals(1, sizeof(1.toByte()))
  }

  @Test
  fun `sizeof Short`() {
    assertEquals(2, sizeof(2.toShort()))
  }

  @Test
  fun `sizeof Boolean`() {
    assertEquals(1, sizeof(true))
  }

  @Test
  fun `sizeof Int`() {
    assertEquals(4, sizeof(4))
  }

  @Test
  fun `sizeof Long`() {
    assertEquals(8, sizeof(45.toLong()))
  }

  @Test
  fun `sizeof Float`() {
    assertEquals(4, sizeof(1f))
  }

  @Test
  fun `sizeof Double`() {
    assertEquals(8, sizeof(1.0))
  }

  @Test
  fun `sizeof String`() {
    assertEquals(6, sizeof("1"))
  }

  @Test
  fun `sizeof empty String`() {
    assertEquals(1, sizeof(""))
  }

  @Test
  fun `sizeof null String`() {
    assertEquals(1, sizeof<String>(null))
  }

  @Test
  fun `sizeof ByteArray`() {
    assertEquals(6, sizeof(ByteArray(1)))
  }

  @Test
  fun `sizeof empty ByteArray`() {
    assertEquals(1, sizeof(ByteArray(0)))
  }

  @Test
  fun `sizeof null ByteArray`() {
    assertEquals(1, sizeof<ByteArray>(null))
  }

  @Test
  fun `sizeof Status`() {
    assertEquals(2, sizeof(Status.Ok))
  }

  @Test
  fun `sizeof PublicUserInChat`() {
    assertEquals(7, sizeof(PublicUserInChat("1")))
  }

  @Test
  fun `sizeof PublicUserInChat with empty userName`() {
    assertEquals(2, sizeof(PublicUserInChat("")))
  }

  @Test
  fun `sizeof PublicChatRoom`() {
    assertEquals(14, sizeof(ChatRoomData("1", "2", true)))
  }

  @Test
  fun `sizeof PublicChatRoom with empty chatRoomName`() {
    assertEquals(9, sizeof(ChatRoomData("", "1", false)))
  }

  @Test
  fun `sizeof TextChatMessage`() {
    assertEquals(23, sizeof(TextChatMessage(true, 1, 1, "1", "1")))
  }

  @Test
  fun `sizeof TextChatMessage with empty senderName`() {
    assertEquals(18, sizeof(TextChatMessage(false, 1, 1, "", "1")))
  }

  @Test
  fun `sizeof TextChatMessage with empty message`() {
    assertEquals(18, sizeof(TextChatMessage(true, 1, 1, "1", "")))
  }

  @Test
  fun `sizeof TextChatMessage with empty senderName and message`() {
    assertEquals(13, sizeof(TextChatMessage(false, 1, 1, "", "")))
  }
}