package core.byte_sink

import core.interfaces.CanBeDrainedToSink
import java.io.ByteArrayInputStream
import java.io.DataInputStream

class InMemoryByteSink private constructor(
  private var array: ByteArray
) : ByteSink() {

  override fun resizeIfNeeded(dataToWriteSize: Int) {
    if (writePosition.get() + dataToWriteSize > array.size) {
      val newArray = ByteArray((array.size * 1.5f).toInt() + dataToWriteSize)
      System.arraycopy(array, 0, newArray, 0, array.size)

      array = newArray
    }
  }

  override fun getReaderPosition() = readPosition.get()
  override fun getWriterPosition() = writePosition.get()

  override fun getStream(): DataInputStream {
    return DataInputStream(ByteArrayInputStream(array))
  }

  fun getArray(): ByteArray {
    return array.copyOfRange(0, writePosition.get())
  }

  override fun readBoolean(): Boolean {
    return array[readPosition.getAndIncrement()] == 1.toByte()
  }

  override fun writeBoolean(boolean: Boolean) {
    resizeIfNeeded(1)

    val boolValue = if (boolean) 1 else 0
    array[writePosition.getAndIncrement()] = boolValue.toByte()
  }

  override fun readByte(): Byte {
    return array[readPosition.getAndIncrement()]
  }

  override fun writeByte(byte: Byte) {
    resizeIfNeeded(1)
    array[writePosition.getAndIncrement()] = byte
  }

  override fun readByteAsInt(): Int {
    return readByte().toInt()
  }

  override fun writeByte(byte: Int) {
    resizeIfNeeded(1)
    array[writePosition.getAndIncrement()] = (byte and 0x000000FF).toByte()
  }

  override fun readShort(): Short {
    var result: Int = 0

    for (i in 0..1) {
      result = result shl 8
      result = result or (array[readPosition.getAndIncrement()].toInt() and 0xFF)
    }

    return result.toShort()
  }

  override fun writeShort(short: Short) {
    resizeIfNeeded(2)
    array[writePosition.getAndIncrement()] = ((short.toInt() shr 8) and 0x000000FF).toByte()
    array[writePosition.getAndIncrement()] = (short.toInt() and 0x000000FF).toByte()
  }

  override fun readShortAsInt(): Int {
    return readShort().toInt()
  }

  override fun writeShort(short: Int) {
    resizeIfNeeded(2)
    array[writePosition.getAndIncrement()] = ((short shr 8) and 0x000000FF).toByte()
    array[writePosition.getAndIncrement()] = (short and 0x000000FF).toByte()
  }

  override fun readInt(): Int {
    var result: Int = 0

    for (i in 0..3) {
      result = result shl 8
      result = result or (array[readPosition.getAndIncrement()].toInt() and 0xFF)
    }

    return result
  }

  override fun writeInt(int: Int) {
    resizeIfNeeded(4)
    array[writePosition.getAndIncrement()] = ((int shr 24) and 0x000000FF).toByte()
    array[writePosition.getAndIncrement()] = ((int shr 16) and 0x000000FF).toByte()
    array[writePosition.getAndIncrement()] = ((int shr 8) and 0x000000FF).toByte()
    array[writePosition.getAndIncrement()] = (int and 0x000000FF).toByte()
  }

  override fun readLong(): Long {
    var result: Long = 0

    for (i in 0..7) {
      result = result shl 8
      result = result or (array[readPosition.getAndIncrement()].toLong() and 0xFF)
    }

    return result
  }

  override fun writeLong(long: Long) {
    resizeIfNeeded(8)
    array[writePosition.getAndIncrement()] = ((long shr 56) and 0x000000FF).toByte()
    array[writePosition.getAndIncrement()] = ((long shr 48) and 0x000000FF).toByte()
    array[writePosition.getAndIncrement()] = ((long shr 40) and 0x000000FF).toByte()
    array[writePosition.getAndIncrement()] = ((long shr 32) and 0x000000FF).toByte()
    array[writePosition.getAndIncrement()] = ((long shr 24) and 0x000000FF).toByte()
    array[writePosition.getAndIncrement()] = ((long shr 16) and 0x000000FF).toByte()
    array[writePosition.getAndIncrement()] = ((long shr 8) and 0x000000FF).toByte()
    array[writePosition.getAndIncrement()] = (long and 0x000000FF).toByte()
  }

  override fun readByteArray(): ByteArray {
    val size = readInt()

    return array.copyOfRange(readPosition.get(), readPosition.get() + size)
      .also { readPosition.getAndAdd(size) }
  }

  override fun writeByteArray(inArray: ByteArray) {
    writeInt(inArray.size)

    resizeIfNeeded(inArray.size)
    System.arraycopy(inArray, 0, array, writePosition.get(), inArray.size)

    writePosition.getAndAdd(inArray.size)
  }

  override fun readString(): String? {
    return if (readByte() == NO_VALUE) {
      null
    } else {
      String(readByteArray())
    }
  }

  override fun writeString(string: String?) {
    if (string == null) {
      writeByte(NO_VALUE)
    } else {
      writeByte(HAS_VALUE)
      writeByteArray(string.toByteArray())
    }
  }

  override fun writeList(listOfObjects: List<CanBeDrainedToSink>?) {
    if (listOfObjects == null) {
      writeByte(NO_VALUE)
    } else {
      writeShort(listOfObjects.size.toShort())

      listOfObjects.forEach { it.serialize(this) }
    }
  }

  override fun close() {
    //fill array with junk bytes before GCing
    array.fill(0xFF.toByte())
  }

  companion object {
    fun fromArray(array: ByteArray): InMemoryByteSink {
      return InMemoryByteSink(array)
    }

    fun createWithInitialSize(size: Int): InMemoryByteSink {
      return InMemoryByteSink(ByteArray(size))
    }
  }
}