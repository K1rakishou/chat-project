package core

import java.util.concurrent.atomic.AtomicInteger

class PositionAwareByteArray private constructor(
  private var array: ByteArray
) {
  private val NO_VALUE = 0.toByte()
  private val HAS_VALUE = 1.toByte()

  private val readPosition = AtomicInteger(0)
  private val writePosition = AtomicInteger(0)

  private fun resizeIfNeeded(dataToWriteSize: Int) {
    if (writePosition.get() + dataToWriteSize > array.size) {
      val newArray = ByteArray((array.size * 1.5f).toInt() + dataToWriteSize)
      System.arraycopy(array, 0, newArray, 0, array.size)

      array = newArray
    }
  }

  fun getReaderPosition() = readPosition.get()
  fun getWriterPosition() = writePosition.get()
  fun getArray() = array.copyOf(writePosition.get())
  fun getLength() = array.size

  fun readBoolean(): Boolean {
    return array[readPosition.getAndIncrement()] == 1.toByte()
  }

  fun writeBoolean(boolean: Boolean) {
    resizeIfNeeded(1)

    val boolValue = if (boolean) 1 else 0
    array[writePosition.getAndIncrement()] = boolValue.toByte()
  }

  fun readByte(): Byte {
    return array[readPosition.getAndIncrement()]
  }

  fun writeByte(byte: Byte) {
    resizeIfNeeded(1)
    array[writePosition.getAndIncrement()] = byte
  }

  fun readByteAsInt(): Int {
    return readByte().toInt()
  }

  fun writeByte(byte: Int) {
    resizeIfNeeded(1)
    array[writePosition.getAndIncrement()] = (byte and 0x000000FF).toByte()
  }

  fun readShort(): Short {
    var result: Int = 0

    for (i in 0..1) {
      result = result shl 8
      result = result or (array[readPosition.getAndIncrement()].toInt() and 0xFF)
    }

    return result.toShort()
  }

  fun writeShort(short: Short) {
    resizeIfNeeded(2)
    array[writePosition.getAndIncrement()] = ((short.toInt() shr 8) and 0x000000FF).toByte()
    array[writePosition.getAndIncrement()] = (short.toInt() and 0x000000FF).toByte()
  }

  fun readShortAsInt(): Int {
    return readShort().toInt()
  }

  fun writeShort(short: Int) {
    resizeIfNeeded(2)
    array[writePosition.getAndIncrement()] = ((short shr 8) and 0x000000FF).toByte()
    array[writePosition.getAndIncrement()] = (short and 0x000000FF).toByte()
  }

  fun readInt(): Int {
    var result: Int = 0

    for (i in 0..3) {
      result = result shl 8
      result = result or (array[readPosition.getAndIncrement()].toInt() and 0xFF)
    }

    return result
  }

  fun writeInt(int: Int) {
    resizeIfNeeded(4)
    array[writePosition.getAndIncrement()] = ((int shr 24) and 0x000000FF).toByte()
    array[writePosition.getAndIncrement()] = ((int shr 16) and 0x000000FF).toByte()
    array[writePosition.getAndIncrement()] = ((int shr 8) and 0x000000FF).toByte()
    array[writePosition.getAndIncrement()] = (int and 0x000000FF).toByte()
  }

  fun readLong(): Long {
    var result: Long = 0

    for (i in 0..7) {
      result = result shl 8
      result = result or (array[readPosition.getAndIncrement()].toLong() and 0xFF)
    }

    return result
  }

  fun writeLong(long: Long) {
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

  fun readByteArray(): ByteArray {
    val size = readInt()

    return array.copyOfRange(readPosition.get(), readPosition.get() + size)
      .also { readPosition.getAndAdd(size) }
  }

  fun writeByteArray(inArray: ByteArray) {
    writeInt(inArray.size)

    resizeIfNeeded(inArray.size)
    System.arraycopy(inArray, 0, array, writePosition.get(), inArray.size)

    writePosition.getAndAdd(inArray.size)
  }

  fun readString(): String? {
    return if (readByte() == NO_VALUE) {
      null
    } else {
      String(readByteArray())
    }
  }

  fun writeString(string: String?) {
    if (string == null) {
      writeByte(NO_VALUE)
    } else {
      writeByte(HAS_VALUE)
      writeByteArray(string.toByteArray())
    }
  }

  companion object {
    fun fromArray(array: ByteArray): PositionAwareByteArray {
      return PositionAwareByteArray(array)
    }

    fun createWithInitialSize(size: Int): PositionAwareByteArray {
      return PositionAwareByteArray(ByteArray(size))
    }
  }
}