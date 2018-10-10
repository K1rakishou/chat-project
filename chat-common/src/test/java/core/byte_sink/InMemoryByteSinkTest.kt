package core.byte_sink

import core.exception.ByteSinkBufferOverflowException
import core.exception.MaxListSizeExceededException
import core.model.drainable.PublicChatRoom
import core.model.drainable.PublicUserInChat
import core.security.SecurityUtils
import org.junit.Assert
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InMemoryByteSinkTest {
  lateinit var byteSink: InMemoryByteSink

  @Before
  fun setUp() {
    byteSink = InMemoryByteSink.createWithInitialSize(32)
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
    assertArrayEquals(byteArray, byteSink.readByteArray(byteArray.size))
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
      PublicUserInChat("123", ByteArray(10) { 0xBB.toByte() }, ByteArray(10) { 0xEE.toByte() }),
      PublicUserInChat("234", ByteArray(10) { 0xAA.toByte() }, ByteArray(10) { 0xFF.toByte() }),
      PublicUserInChat("345", ByteArray(10) { 0xCC.toByte() }, ByteArray(10) { 0x12.toByte() }),
      PublicUserInChat("456", ByteArray(10) { 0xDD.toByte() }, ByteArray(10) { 0x43.toByte() })
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
  fun testWriteByteSink() {
    val string = "345467d57 45uwr6jr6j r67j r6j"
    val boolean = true
    val long = 0x11223344556677

    val newByteSink = InMemoryByteSink.createWithInitialSize(4)
    newByteSink.writeString(string)
    newByteSink.writeBoolean(boolean)
    newByteSink.writeLong(long)

    byteSink.writeByteSink(newByteSink)
    byteSink.writeByteSink(newByteSink)
    byteSink.writeByteSink(newByteSink)
    byteSink.writeByteSink(newByteSink)

    for (i in 0 until 4) {
      assertEquals(string, byteSink.readString(string.length))
      assertEquals(boolean, byteSink.readBoolean())
      assertEquals(long, byteSink.readLong())
    }
  }

  @Test
  fun testRawReadWrite() {
    InMemoryByteSink.createWithInitialSize(128).use { bs ->
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
  fun testReadWriteDrainable() {
    val expectedDrainable = PublicUserInChat("test user", ByteArray(255) { 0x44.toByte() }, ByteArray(522) { 0x22.toByte() })

    byteSink.writeDrainable(expectedDrainable)
    val actualDrainable = byteSink.readDrainable<PublicUserInChat>(PublicUserInChat::class)

    assertEquals(expectedDrainable.userName, actualDrainable!!.userName)
    assertArrayEquals(expectedDrainable.rootPublicKey, actualDrainable!!.rootPublicKey)
    assertArrayEquals(expectedDrainable.sessionPublicKey, actualDrainable!!.sessionPublicKey)
  }

  @Test
  fun testReadWriteNullDrainable() {
    byteSink.writeDrainable(null)
    assertNull(byteSink.readDrainable<PublicChatRoom>(PublicChatRoom::class))
  }

  @Test
  fun testReadWriteListOfDrainables() {
    val expectedListOfDrainables = listOf(
      PublicChatRoom("s5sdhe6e46je46j", 22),
      PublicChatRoom("a35h35jw35kk56k", 51),
      PublicChatRoom("s5sdhe6e4576je46j", 34),
      PublicChatRoom("64hwhw4hj54hj", 15),
      PublicChatRoom("fg6yfdt7futu", 5),
      PublicChatRoom("46", 2)
    )

    byteSink.writeList(expectedListOfDrainables)
    val actualListOfDrainables = byteSink.readList<PublicChatRoom>(PublicChatRoom::class, expectedListOfDrainables.size)

    for (i in 0 until expectedListOfDrainables.size) {
      assertEquals(expectedListOfDrainables[i].chatRoomName, actualListOfDrainables[i].chatRoomName)
      assertEquals(expectedListOfDrainables[i].usersCount, actualListOfDrainables[i].usersCount)
    }
  }

  @Test
  fun testGetArray() {
    byteSink.writeString("1234567890")
    val byteSinkArray = byteSink.getArray()

    assertEquals(15, byteSinkArray.size)
  }

  @Test
  fun testByteSinkEncryption() {
    val size = 16384
    val key = ByteArray(32) { 0xAA.toByte() }
    val iv = ByteArray(24) { 0xBB.toByte() }

    InMemoryByteSink.createWithInitialSize(size).use { bs ->
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