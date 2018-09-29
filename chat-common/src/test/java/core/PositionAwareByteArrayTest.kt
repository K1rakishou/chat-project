package core

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class PositionAwareByteArrayTest {
  lateinit var array: PositionAwareByteArray

  @Before
  fun setUp() {
    array = PositionAwareByteArray.createWithInitialSize(32)
  }

  @Test
  fun testReadWriteByte() {
    array.writeByte(12)

    assertEquals(1, array.getWriterPosition())
    assertEquals(12, array.readByte())
    assertEquals(1, array.getReaderPosition())
  }

  @Test
  fun testReadWriteShort() {
    array.writeShort(1122)

    assertEquals(2, array.getWriterPosition())
    assertEquals(1122, array.readShort())
    assertEquals(2, array.getReaderPosition())
  }

  @Test
  fun testReadWriteInt() {
    array.writeInt(11223344)

    assertEquals(4, array.getWriterPosition())
    assertEquals(11223344, array.readInt())
    assertEquals(4, array.getReaderPosition())
  }

  @Test
  fun testReadWriteLong() {
    array.writeLong(1122334455667788)

    assertEquals(8, array.getWriterPosition())
    assertEquals(1122334455667788, array.readLong())
    assertEquals(8, array.getReaderPosition())
  }

  @Test
  fun testReadWriteByteArray() {
    val byteArray = "This is a test string".toByteArray()

    array.writeByteArray(byteArray)

    assertEquals(byteArray.size + 4, array.getWriterPosition())
    Assert.assertArrayEquals(byteArray, array.readByteArray())
    assertEquals(byteArray.size + 4, array.getReaderPosition())
  }

  @Test
  fun testReadWriteString() {
    val string = "This is a test string"

    array.writeString(string)

    assertEquals(string.length + 4 + 1, array.getWriterPosition())
    assertEquals(string, array.readString())
    assertEquals(string.length + 4 + 1, array.getReaderPosition())
  }

  @Test
  fun testReadWringNullString() {
    array.writeString(null)

    assertEquals(1, array.getWriterPosition())
    assertEquals(null, array.readString())
    assertEquals(1, array.getReaderPosition())
  }

  @Test
  fun testResizing() {
    val string = "1234567890123456789012345678901234567890123456789012345678901234567890"
    array.writeString(string)
    array.writeString(string)
    array.writeString(string)
    array.writeString(string)

    assertEquals(string, array.readString())
    assertEquals(string, array.readString())
    assertEquals(string, array.readString())
    assertEquals(string, array.readString())
  }
}