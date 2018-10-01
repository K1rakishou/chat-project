package core.byte_sink

import java.util.concurrent.atomic.AtomicInteger

abstract class ByteSink {
  protected val NO_VALUE = 0.toByte()
  protected val HAS_VALUE = 1.toByte()

  protected val readPosition = AtomicInteger(0)
  protected val writePosition = AtomicInteger(0)

  protected abstract fun resizeIfNeeded(dataToWriteSize: Int)

  abstract fun getReaderPosition(): Int
  abstract fun getWriterPosition(): Int

  abstract fun getArray(): ByteArray
  abstract fun getLength(): Int

  abstract fun readBoolean(): Boolean
  abstract fun writeBoolean(boolean: Boolean)
  abstract fun readByte(): Byte
  abstract fun writeByte(byte: Byte)
  abstract fun readByteAsInt(): Int
  abstract fun writeByte(byte: Int)
  abstract fun readShort(): Short
  abstract fun writeShort(short: Short)
  abstract fun readShortAsInt(): Int
  abstract fun writeShort(short: Int)
  abstract fun readInt(): Int
  abstract fun writeInt(int: Int)
  abstract fun readLong(): Long
  abstract fun writeLong(long: Long)
  abstract fun readByteArray(): ByteArray
  abstract fun writeByteArray(inArray: ByteArray)
  abstract fun readString(): String?
  abstract fun writeString(string: String?)

  abstract fun release()
}