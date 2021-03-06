package core.extensions

import core.*
import core.byte_sink.InMemoryByteSink
import core.byte_sink.OnDiskByteSink
import core.utils.TimeUtils
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.readFully
import java.io.File

suspend fun ByteReadChannel.readPacketInfo(byteSinkFileCachePath: String, bodySize: Int): PacketInfo {
  if (bodySize <= Constants.maxInMemoryByteSinkSize) {
    val packetType = PacketType.fromShort(readShort())
    val sink = InMemoryByteSink.createWithInitialSize(Constants.maxInMemoryByteSinkSize)
    val array = ByteArray(bodySize)

    readFully(array)
    sink.writeByteArrayRaw(0, array)

    return PacketInfo(packetType, sink)
  } else {
    val file = File("$byteSinkFileCachePath\\test_file-${TimeUtils.getCurrentTime()}.tmp")
    if (!file.exists()) {
      file.createNewFile()
    }

    val sink = OnDiskByteSink.fromFile(file, bodySize)

    for (offset in 0 until bodySize step Constants.maxInMemoryByteSinkSize) {
      val chunk = if (bodySize - offset > Constants.maxInMemoryByteSinkSize) {
        Constants.maxInMemoryByteSinkSize
      } else {
        bodySize - offset
      }

      val array = ByteArray(chunk)
      readFully(array)
      sink.writeByteArrayRaw(offset, array)
    }

    val packetType = PacketType.fromShort(sink.readShort())
    return PacketInfo(packetType, sink)
  }
}

suspend fun ByteReadChannel.readResponseInfo(byteSinkFileCachePath: String, bodySize: Int): ResponseInfo {
  if (bodySize <= Constants.maxInMemoryByteSinkSize) {
    val packetType = ResponseType.fromShort(readShort())
    val sink = InMemoryByteSink.createWithInitialSize(Constants.maxInMemoryByteSinkSize)
    val array = ByteArray(bodySize)

    readFully(array)
    sink.writeByteArrayRaw(0, array)

    return ResponseInfo(packetType, sink)
  } else {
    val file = File("$byteSinkFileCachePath\\test_file-${TimeUtils.getCurrentTime()}.tmp")
    if (!file.exists()) {
      file.createNewFile()
    }

    val sink = OnDiskByteSink.fromFile(file, bodySize)

    for (offset in 0 until bodySize step Constants.maxInMemoryByteSinkSize) {
      val chunk = if (bodySize - offset > Constants.maxInMemoryByteSinkSize) {
        Constants.maxInMemoryByteSinkSize
      } else {
        bodySize - offset
      }

      val array = ByteArray(chunk)
      readFully(array)
      sink.writeByteArrayRaw(offset, array)
    }

    val packetType = ResponseType.fromShort(sink.readShort())
    return ResponseInfo(packetType, sink)
  }
}