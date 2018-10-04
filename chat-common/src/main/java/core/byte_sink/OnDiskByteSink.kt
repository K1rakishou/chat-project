package core.byte_sink

import core.Constants.MAX_PACKET_SIZE_FOR_MEMORY_HANDLING
import core.exception.ByteSinkReadException
import core.interfaces.CanBeDrainedToSink
import core.interfaces.CanMeasureSizeOfFields
import core.sizeof
import java.io.*
import java.lang.IllegalArgumentException

class OnDiskByteSink private constructor(
  private val file: File,
  private val defaultLen: Long
) : ByteSink() {
  private val raf: RandomAccessFile

  init {
    if (file.isDirectory) {
      throw IllegalArgumentException("Input file ${file.absolutePath} can not be a directory!")
    }

    raf = RandomAccessFile(file, "rw")

    if (!file.exists()) {
      raf.setLength(defaultLen)
    }

  }

  override fun resizeIfNeeded(dataToWriteSize: Int) {
    val fileLen = raf.length()

    if (writePosition.get() + dataToWriteSize > fileLen) {
      val newLen = (fileLen * 2) + dataToWriteSize
      raf.setLength(newLen)
    }
  }

  override fun getReaderPosition() = readPosition.get()
  override fun getWriterPosition() = writePosition.get()

  override fun getStream(): DataInputStream {
    return DataInputStream(FileInputStream(file))
  }

  override fun readBoolean(): Boolean {
    if (getReaderPosition() + sizeof<Boolean>() > raf.length()) {
      throw ByteSinkReadException()
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
      throw ByteSinkReadException()
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
      throw ByteSinkReadException()
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
      throw ByteSinkReadException()
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
      throw ByteSinkReadException()
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
      throw ByteSinkReadException()
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
      throw ByteSinkReadException()
    }

    raf.seek(readPosition.getAndAdd(sizeof<Long>()).toLong())
    return raf.readLong()
  }

  override fun writeLong(long: Long) {
    raf.seek(writePosition.getAndAdd(sizeof<Long>()).toLong())
    raf.writeLong(long)
  }

  override fun readByteArray(): ByteArray {
    val arrayLen = readInt()

    if (getReaderPosition() + arrayLen > raf.length()) {
      throw ByteSinkReadException()
    }

    val array = ByteArray(arrayLen)
    raf.read(array)
    readPosition.getAndAdd(arrayLen)

    return array
  }

  override fun writeByteArray(inArray: ByteArray) {
    writeInt(inArray.size)
    raf.write(inArray)

    writePosition.getAndAdd(inArray.size)
  }

  override fun readString(): String? {
    raf.seek(readPosition.get().toLong())

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

  override fun <T> writeList(listOfObjects: List<T>?)
    where T : CanBeDrainedToSink,
          T : CanMeasureSizeOfFields {
    if (listOfObjects == null) {
      writeByte(NO_VALUE)
    } else {
      writeShort(listOfObjects.size.toShort())

      listOfObjects.forEach { it.serialize(this) }
    }
  }

  override fun close() {
    //rewrite file with junk bytes before deleting
    val array = ByteArray(raf.length().toInt()) { 0xFF.toByte()}

    raf.write(array)
    raf.close()

    file.delete()
  }

  companion object {
    fun fromFile(filePath: File, initialSize: Int = MAX_PACKET_SIZE_FOR_MEMORY_HANDLING): OnDiskByteSink {
      return OnDiskByteSink(filePath, initialSize.toLong())
    }
  }
}