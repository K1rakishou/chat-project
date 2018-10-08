package core.byte_sink

import core.exception.ByteSinkBufferOverflowException
import core.exception.MaxListSizeExceededException
import core.model.drainable.PublicUserInChat
import core.security.SecurityUtils
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

    assertEquals(byteArray.size + 4 + 1, byteSink.getWriterPosition())
    Assert.assertArrayEquals(byteArray, byteSink.readByteArray(byteArray.size))
    assertEquals(byteArray.size + 4 + 1, byteSink.getReaderPosition())
  }

  @Test
  fun testReadWriteString() {
    val string = "This is a test string"

    byteSink.writeString(string)

    assertEquals(string.length + 4 + 1, byteSink.getWriterPosition())
    assertEquals(string, byteSink.readString(string.length))
    assertEquals(string.length + 4 + 1, byteSink.getReaderPosition())
  }

  @Test
  fun testReadWringNullString() {
    byteSink.writeString(null)

    assertEquals(1, byteSink.getWriterPosition())
    assertEquals(null, byteSink.readString(1))
    assertEquals(1, byteSink.getReaderPosition())
  }

  @Test
  fun testResizing() {
    val string = "1234567890123456789012345678901234567890123456789012345678901234567890"
    byteSink.writeString(string)
    byteSink.writeString(string)
    byteSink.writeString(string)
    byteSink.writeString(string)

    assertEquals(string, byteSink.readString(string.length))
    assertEquals(string, byteSink.readString(string.length))
    assertEquals(string, byteSink.readString(string.length))
    assertEquals(string, byteSink.readString(string.length))
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
    Assert.assertArrayEquals(byteArray, byteSink.readByteArray(byteArray.size))
    assertEquals(string, byteSink.readString(string.length))
  }

  @Test(expected = ByteSinkBufferOverflowException::class)
  fun testExceedByteArraySize() {
    val byteArray = ByteArray(10) { 0xAA.toByte() }

    byteSink.writeByteArray(byteArray)
    byteSink.readByteArray(5)
  }

  @Test(expected = ByteSinkBufferOverflowException::class)
  fun testExceedStringSize() {
    val string = "45436734734838"

    byteSink.writeString(string)
    byteSink.readString(5)
  }

  @Test(expected = MaxListSizeExceededException::class)
  fun testExceedListSize() {
    val list = listOf(
      PublicUserInChat("123", ByteArray(10) { 0xBB.toByte() }),
      PublicUserInChat("234", ByteArray(10) { 0xAA.toByte() }),
      PublicUserInChat("345", ByteArray(10) { 0xCC.toByte() }),
      PublicUserInChat("456", ByteArray(10) { 0xDD.toByte() })
    )

    byteSink.writeList(list)
    byteSink.readList<PublicUserInChat>(PublicUserInChat::class, 2)
  }

  @Test
  fun testWriteReadEmptyList() {
    val list = listOf<PublicUserInChat>()

    byteSink.writeList(list)
    byteSink.readList<PublicUserInChat>(PublicUserInChat::class, 0)
  }

  @Test
  fun testRawReadWrite() {
    val file = File.createTempFile("temp", "file")
    file.createNewFile()

    OnDiskByteSink.fromFile(file, 128).use { bs ->
      val testArray1 = ByteArray(32) { 0x11.toByte() }
      val testArray2 = ByteArray(32) { 0x22.toByte() }
      val testArray3 = ByteArray(32) { 0x33.toByte() }
      val testArray4 = ByteArray(32) { 0x44.toByte() }

      bs.writeByteArrayRaw(0, testArray1)
      bs.writeByteArrayRaw(32, testArray2)
      bs.writeByteArrayRaw(64, testArray3)
      bs.writeByteArrayRaw(96, testArray4)

      assertArrayEquals(testArray1, bs.readByteArrayRaw(0, 32))
      assertArrayEquals(testArray2, bs.readByteArrayRaw(32, 32))
      assertArrayEquals(testArray3, bs.readByteArrayRaw(64, 32))
      assertArrayEquals(testArray4, bs.readByteArrayRaw(96, 32))
    }
  }

  @Test
  fun testByteSinkEncryption() {
    val size = 16384

    val file = File.createTempFile("temp", "file")
    file.createNewFile()
    val key = ByteArray(32) { 0xAA.toByte() }
    val iv = ByteArray(24) { 0xBB.toByte() }

    OnDiskByteSink.fromFile(file, size).use { bs ->
      val testArrays = mutableListOf<ByteArray>()

      for (i in 0 until size step 32) {
        testArrays += ByteArray(32) { 0x11.toByte() }
      }

      for ((index, array) in testArrays.withIndex()) {
        bs.writeByteArrayRaw(index * 32, array)
      }

      for (i in 0 until size step 32) {
        val chunk = bs.readByteArrayRaw(i, 32)
        val encryptedChunk = SecurityUtils.Encryption.xSalsa20Encrypt(key, iv, chunk)

        bs.writeByteArrayRaw(i, encryptedChunk)
      }

      for (i in 0 until size step 32) {
        val chunk = bs.readByteArrayRaw(i, 32)
        val encryptedChunk = SecurityUtils.Encryption.xSalsa20Decrypt(key, iv, chunk)

        bs.writeByteArrayRaw(i, encryptedChunk)
      }

      for ((index, array) in testArrays.withIndex()) {
        assertArrayEquals(array, bs.readByteArrayRaw(index * 32, 32))
      }
    }

    assertEquals(false, file.exists())
  }

  @Test
  fun testByteSinkEncryption2() {
    val size = 16384

    val file = File.createTempFile("temp", "file")
    file.createNewFile()
    val key = ByteArray(32) { 0xAA.toByte() }
    val iv = ByteArray(24) { 0xBB.toByte() }

    OnDiskByteSink.fromFile(file, size).use { bs ->
      val testArrays = mutableListOf<ByteArray>()

      for (i in 0 until size step 32) {
        testArrays += ByteArray(32) { 0x11.toByte() }
      }

      for ((index, array) in testArrays.withIndex()) {
        bs.writeByteArrayRaw(index * 32, array)
      }

      SecurityUtils.Encryption.xSalsa20Encrypt(key, iv, bs, size)
      SecurityUtils.Encryption.xSalsa20Decrypt(key, iv, bs, size)

      for ((index, array) in testArrays.withIndex()) {
        assertArrayEquals(array, bs.readByteArrayRaw(index * 32, 32))
      }
    }
  }
}