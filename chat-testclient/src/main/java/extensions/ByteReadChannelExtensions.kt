package extensions

import core.Constants
import core.ResponseInfo
import core.ResponseType
import core.byte_sink.InMemoryByteSink
import core.byte_sink.OnDiskByteSink
import core.utils.TimeUtils
import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.readFully
import java.io.File

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