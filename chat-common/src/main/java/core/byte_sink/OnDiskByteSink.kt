package core.byte_sink

import core.Constants.maxInMemoryByteSinkSize
import core.exception.*
import core.interfaces.CanBeDrainedToSink
import core.interfaces.CanMeasureSizeOfFields
import core.model.drainable.DrainableFactory
import core.sizeof
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.nio.file.Files
import kotlin.reflect.KClass

class OnDiskByteSink private constructor(
  private val file: File,
  private val defaultLen: Long
) : ByteSink() {
  private val raf: RandomAccessFile

  init {
    isByteSinkClosed = false

    if (file.isDirectory) {
      throw IllegalArgumentException("Input file ${file.absolutePath} can not be a directory!")
    }

    if (!file.exists()) {
      throw IllegalStateException("File does not exist ${file.absolutePath}")
    }

    raf = RandomAccessFile(file, "rw")
    raf.setLength(defaultLen)
    raf.seek(0)
  }

  override fun isClosed(): Boolean = isByteSinkClosed

  override fun resizeIfNeeded(dataToWriteSize: Int) {
    val fileLen = raf.length()

    if (writePosition.get() + dataToWriteSize > fileLen) {
      val newLen = (fileLen * 2) + dataToWriteSize
      raf.setLength(newLen)
    }
  }

  override fun getReaderPosition() = readPosition.get()
  override fun setReaderPosition(position: Int) = readPosition.set(position)

  override fun getWriterPosition() = writePosition.get()
  override fun setWriterPosition(position: Int) = writePosition.set(position)

  override fun getStream(): DataInputStream {
    return DataInputStream(FileInputStream(file))
  }

  //Use only for tests! May cause OOM!!!
  override fun getArray(from: Int, to: Int): ByteArray {
    val start = if (from != -1) {
      from
    } else {
      0
    }

    val end = if (to != -1) {
      to
    } else {
      writePosition.get()
    }

    require(start < end) { "start ($start) >= end ($end)" }

    val array = ByteArray(end - start)
    getStream().use { stream ->
      stream.read(array)
    }

    return array
  }

  override fun readBoolean(): Boolean {
    if (getReaderPosition() + sizeof<Boolean>() > raf.length()) {
      throw ReaderPositionExceededBufferSizeException(getReaderPosition(), sizeof<Boolean>(), raf.length())
    }

    raf.seek(readPosition.getAndIncrement().toLong())
    return raf.readBoolean()
  }

  override fun writeBoolean(boolean: Boolean) {
    raf.seek(writePosition.getAndIncrement().toLong())
    raf.writeBoolean(boolean)
  }

  override fun readByte(): Byte {
    if (getReaderPosition() + sizeof<Byte>() > raf.length()) {
      throw ReaderPositionExceededBufferSizeException(getReaderPosition(), sizeof<Byte>(), raf.length())
    }

    raf.seek(readPosition.getAndIncrement().toLong())
    return raf.readByte()
  }

  override fun writeByte(byte: Byte) {
    raf.seek(writePosition.getAndIncrement().toLong())
    raf.write(byte.toInt())
  }

  override fun readByteAsInt(): Int {
    if (getReaderPosition() + sizeof<Byte>() > raf.length()) {
      throw ReaderPositionExceededBufferSizeException(getReaderPosition(), sizeof<Byte>(), raf.length())
    }

    raf.seek(readPosition.getAndIncrement().toLong())
    return raf.readByte().toInt()
  }

  override fun writeByte(byte: Int) {
    raf.seek(writePosition.getAndIncrement().toLong())
    raf.writeByte(byte)
  }

  override fun readShort(): Short {
    if (getReaderPosition() + sizeof<Short>() > raf.length()) {
      throw ReaderPositionExceededBufferSizeException(getReaderPosition(), sizeof<Short>(), raf.length())
    }

    raf.seek(readPosition.getAndAdd(sizeof<Short>()).toLong())
    return raf.readShort()
  }

  override fun writeShort(short: Short) {
    raf.seek(writePosition.getAndAdd(sizeof<Short>()).toLong())
    raf.writeShort(short.toInt())
  }

  override fun readShortAsInt(): Int {
    if (getReaderPosition() + sizeof<Short>() > raf.length()) {
      throw ReaderPositionExceededBufferSizeException(getReaderPosition(), sizeof<Short>(), raf.length())
    }

    raf.seek(readPosition.getAndAdd(sizeof<Short>()).toLong())
    return raf.readShort().toInt()
  }

  override fun writeShort(short: Int) {
    raf.seek(writePosition.getAndAdd(sizeof<Short>()).toLong())
    raf.writeShort(short)
  }

  override fun readInt(): Int {
    if (getReaderPosition() + sizeof<Int>() > raf.length()) {
      throw ReaderPositionExceededBufferSizeException(getReaderPosition(), sizeof<Int>(), raf.length())
    }

    raf.seek(readPosition.getAndAdd(sizeof<Int>()).toLong())
    return raf.readInt()
  }

  override fun writeInt(int: Int) {
    raf.seek(writePosition.getAndAdd(sizeof<Int>()).toLong())
    raf.writeInt(int)
  }

  override fun readLong(): Long {
    if (getReaderPosition() + sizeof<Long>() > raf.length()) {
      throw ReaderPositionExceededBufferSizeException(getReaderPosition(), sizeof<Long>(), raf.length())
    }

    raf.seek(readPosition.getAndAdd(sizeof<Long>()).toLong())
    return raf.readLong()
  }

  override fun writeLong(long: Long) {
    raf.seek(writePosition.getAndAdd(sizeof<Long>()).toLong())
    raf.writeLong(long)
  }

  override fun readByteArray(maxSize: Int): ByteArray? {
    return if (readByte() == NO_VALUE) {
      null
    } else {
      val arrayLen = readInt()
      if (arrayLen > maxSize) {
        throw ByteSinkBufferOverflowException(arrayLen, maxSize)
      }

      if (getReaderPosition() + arrayLen > raf.length()) {
        throw ReaderPositionExceededBufferSizeException(getReaderPosition(), arrayLen, raf.length())
      }

      val array = ByteArray(arrayLen)
      raf.read(array)
      readPosition.getAndAdd(arrayLen)

      array
    }
  }

  override fun writeByteArray(inArray: ByteArray?) {
    if (inArray == null) {
      writeByte(NO_VALUE)
    } else {
      writeByte(HAS_VALUE)
      writeInt(inArray.size)
      raf.write(inArray)

      writePosition.getAndAdd(inArray.size)
    }
  }

  override fun writeByteArrayRaw(offset: Int, inArray: ByteArray) {
    resizeIfNeeded(inArray.size)

    raf.seek(offset.toLong())
    raf.write(inArray)

    writePosition.getAndAdd(inArray.size)
  }

  override fun rewriteByteArrayRaw(offset: Int, inArray: ByteArray) {
    require(offset + inArray.size <= writePosition.get())

    raf.seek(offset.toLong())
    raf.write(inArray)
  }

  override fun readByteArrayRaw(offset: Int, readAmount: Int): ByteArray {
    raf.seek(offset.toLong())

    val array = ByteArray(readAmount) { 0xFF.toByte() }
    raf.read(array, 0, readAmount)

    return array
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

      val objList = ArrayList<T>(listSize)

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
    if (isByteSinkClosed) {
      return
    }

    //rewrite file with junk bytes before deleting
    val array = ByteArray(raf.length().toInt()) { 0xFF.toByte() }

    raf.seek(0L)
    raf.write(array)
    raf.close()

    Files.deleteIfExists(file.toPath())
    isByteSinkClosed = true
  }

  companion object {
    fun fromFile(filePath: File, initialSize: Int = maxInMemoryByteSinkSize): OnDiskByteSink {
      return OnDiskByteSink(filePath, initialSize.toLong())
    }
  }
}