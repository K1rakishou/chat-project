package core.extensions

import core.Constants
import core.PacketInfo
import core.PacketType
import core.byte_sink.InMemoryByteSink
import core.byte_sink.OnDiskByteSink
import core.utils.TimeUtils
import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.readFully
import kotlinx.io.core.IoBuffer
import kotlinx.io.core.readFully
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