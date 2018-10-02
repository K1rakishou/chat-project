package core.byte_sink

import org.junit.After
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class OnDiskByteSinkTest {
  lateinit var byteSink: OnDiskByteSink

  @Before
  fun setUp() {
    val file = File.createTempFile("temp", "file")
    file.createNewFile()

    byteSink = OnDiskByteSink.fromFile(file, 8)
  }

  @After
  fun tearDown() {
    byteSink.close()
  }

  @Test
  fun testReadWriteByte() {
    byteSink.writeByte(12)

    assertEquals(1, byteSink.getWriterPosition())
    assertEquals(12.toByte(), byteSink.readByte())
    assertEquals(1, byteSink.getReaderPosition())
  }

  @Test
  fun testReadWriteShort() {
    byteSink.writeShort(1122)

    assertEquals(2, byteSink.getWriterPosition())
    assertEquals(1122.toShort(), byteSink.readShort())
    assertEquals(2, byteSink.getReaderPosition())
  }

  @Test
  fun testReadWriteInt() {
    byteSink.writeInt(11223344)

    assertEquals(4, byteSink.getWriterPosition())
    assertEquals(11223344, byteSink.readInt())
    assertEquals(4, byteSink.getReaderPosition())
  }

  @Test
  fun testReadWriteLong() {
    byteSink.writeLong(1122334455667788)

    assertEquals(8, byteSink.getWriterPosition())
    assertEquals(1122334455667788L, byteSink.readLong())
    assertEquals(8, byteSink.getReaderPosition())
  }

  @Test
  fun testReadWriteByteArray() {
    val byteArray = "This is a test string".toByteArray()

    byteSink.writeByteArray(byteArray)

    assertEquals(byteArray.size + 4, byteSink.getWriterPosition())
    Assert.assertArrayEquals(byteArray, byteSink.readByteArray())
    assertEquals(byteArray.size + 4, byteSink.getReaderPosition())
  }

  @Test
  fun testReadWriteString() {
    val string = "This is a test string"

    byteSink.writeString(string)

    assertEquals(string.length + 4 + 1, byteSink.getWriterPosition())
    assertEquals(string, byteSink.readString())
    assertEquals(string.length + 4 + 1, byteSink.getReaderPosition())
  }

  @Test
  fun testReadWringNullString() {
    byteSink.writeString(null)

    assertEquals(1, byteSink.getWriterPosition())
    assertEquals(null, byteSink.readString())
    assertEquals(1, byteSink.getReaderPosition())
  }

  @Test
  fun testResizing() {
    val string = "1234567890123456789012345678901234567890123456789012345678901234567890"
    byteSink.writeString(string)
    byteSink.writeString(string)
    byteSink.writeString(string)
    byteSink.writeString(string)

    assertEquals(string, byteSink.readString())
    assertEquals(string, byteSink.readString())
    assertEquals(string, byteSink.readString())
    assertEquals(string, byteSink.readString())
  }

  @Test
  fun testReadWriteDifferentTypes() {
    val byte = 0x64.toByte()
    val short = 0x5566.toShort()
    val int = 93495435
    val long = 0x88449955332242
    val byteArray = ByteArray(4) { 0x44.toByte() }
    val string = "This is a test string s sT^ 346 s36 "

    byteSink.writeByte(byte)
    byteSink.writeShort(short)
    byteSink.writeInt(int)
    byteSink.writeLong(long)
    byteSink.writeByteArray(byteArray)
    byteSink.writeString(string)

    assertEquals(byte, byteSink.readByte())
    assertEquals(short, byteSink.readShort())
    assertEquals(int, byteSink.readInt())
    assertEquals(long, byteSink.readLong())
    Assert.assertArrayEquals(byteArray, byteSink.readByteArray())
    assertEquals(string, byteSink.readString())
  }
}