package core.byte_sink

import core.exception.*
import core.interfaces.CanBeDrainedToSink
import core.interfaces.CanMeasureSizeOfFields
import core.model.drainable.DrainableFactory
import core.sizeof
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import kotlin.reflect.KClass

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

  override fun getArray(): ByteArray {
    return array.copyOfRange(0, writePosition.get())
  }

  override fun readBoolean(): Boolean {
    if (getReaderPosition() + sizeof<Boolean>() > array.size) {
      throw ReaderPositionExceededBufferSizeException(getReaderPosition(), sizeof<Boolean>(), array.size)
    }

    return array[readPosition.getAndIncrement()] == 1.toByte()
  }

  override fun writeBoolean(boolean: Boolean) {
    resizeIfNeeded(1)

    val boolValue = if (boolean) 1 else 0
    array[writePosition.getAndIncrement()] = boolValue.toByte()
  }

  override fun readByte(): Byte {
    if (getReaderPosition() + sizeof<Byte>() > array.size) {
      throw ReaderPositionExceededBufferSizeException(getReaderPosition(), sizeof<Byte>(), array.size)
    }

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
    if (getReaderPosition() + sizeof<Short>() > array.size) {
      throw ReaderPositionExceededBufferSizeException(getReaderPosition(), sizeof<Short>(), array.size)
    }

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
    if (getReaderPosition() + sizeof<Int>() > array.size) {
      throw ReaderPositionExceededBufferSizeException(getReaderPosition(), sizeof<Int>(), array.size)
    }

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
    if (getReaderPosition() + sizeof<Long>() > array.size) {
      throw ReaderPositionExceededBufferSizeException(getReaderPosition(), sizeof<Long>(), array.size)
    }

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

  override fun readByteArray(maxSize: Int): ByteArray? {
    return if (readByte() == NO_VALUE) {
      null
    } else {
      val arrayLen = readInt()
      if (arrayLen > maxSize) {
        throw ByteSinkBufferOverflowException(arrayLen, maxSize)
      }

      if (getReaderPosition() + arrayLen > array.size) {
        throw ReaderPositionExceededBufferSizeException(getReaderPosition(), arrayLen, array.size)
      }

      return array.copyOfRange(readPosition.get(), readPosition.get() + arrayLen)
        .also { readPosition.getAndAdd(arrayLen) }
    }
  }

  override fun writeByteArray(inArray: ByteArray?) {
    if (inArray == null) {
      writeByte(NO_VALUE)
    } else {
      writeByte(HAS_VALUE)
      writeInt(inArray.size)

      resizeIfNeeded(inArray.size)
      System.arraycopy(inArray, 0, array, writePosition.get(), inArray.size)

      writePosition.getAndAdd(inArray.size)
    }
  }

  override fun writeByteArrayRaw(offset: Int, inArray: ByteArray) {
    System.arraycopy(inArray, 0, array, offset, inArray.size)
  }

  override fun readByteArrayRaw(offset: Int, readAmount: Int): ByteArray {
    return array.copyOfRange(offset, offset + readAmount)
  }

  override fun readString(maxSize: Int): String? {
    val array = readByteArray(maxSize)
    if (array == null) {
      return null
    } else {
      return String(array)
    }
  }

  override fun writeString(string: String?) {
    writeByteArray(string?.toByteArray())
  }

  override fun <T : CanBeDrainedToSink> readList(clazz: KClass<*>, maxSize: Int): List<T> {
    if (readByte() == NO_VALUE) {
      return emptyList()
    } else {
      val listSize = readShort().toInt()
      if (listSize > maxSize) {
        throw MaxListSizeExceededException(listSize, maxSize)
      }

      val objList = mutableListOf<T>()

      for (listIndex in 0 until listSize) {
        val obj = DrainableFactory.fromByteSink<CanBeDrainedToSink>(clazz, this)
        if (obj == null) {
          throw DrainableDeserializationException(clazz, listIndex)
        }

        objList += obj as T
      }
      return objList
    }
  }

  override fun <T> writeList(listOfObjects: List<T>)
    where T : CanBeDrainedToSink,
          T : CanMeasureSizeOfFields {
    if (listOfObjects.isEmpty()) {
      writeByte(NO_VALUE)
    } else {
      writeByte(HAS_VALUE)
      writeShort(listOfObjects.size.toShort())

      val totalSizeForAllObjects = listOfObjects.asSequence()
        .map { it.getSize() }
        .reduce { acc, size -> acc + size }

      resizeIfNeeded(totalSizeForAllObjects)
      listOfObjects.forEach { it.serialize(this) }
    }
  }

  override fun <T : CanBeDrainedToSink> readDrainable(clazz: KClass<*>): T? {
    if (readByte() == NO_VALUE) {
      return null
    } else {
      return DrainableFactory.fromByteSink<CanBeDrainedToSink>(clazz, this) as T
    }
  }

  override fun <T> writeDrainable(obj: T?)
    where T : CanBeDrainedToSink,
          T : CanMeasureSizeOfFields {
    if (obj == null) {
      writeByte(NO_VALUE)
    } else {
      writeByte(HAS_VALUE)

      resizeIfNeeded(obj.getSize())
      obj.serialize(this)
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