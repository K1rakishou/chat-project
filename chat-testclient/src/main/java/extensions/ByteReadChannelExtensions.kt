package extensions

import core.Constants
import core.ResponseInfo
import core.ResponseType
import core.byte_sink.InMemoryByteSink
import core.byte_sink.OnDiskByteSink
import core.utils.TimeUtils
import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.readFully
import kotlinx.io.core.IoBuffer
import kotlinx.io.core.readFully
import java.io.File

suspend fun ByteReadChannel.readResponseInfo(byteSinkFileCachePath: String, bodySize: Int): ResponseInfo {
  if (bodySize <= Constants.maxInMemoryByteSinkSize) {
    return IoBuffer.Pool.autoRelease { buffer ->
      this.readFully(buffer, bodySize)

      val packetType = ResponseType.fromShort(buffer.readShort())
      val packetPayloadRaw = ByteArray(buffer.readRemaining)
      buffer.readFully(packetPayloadRaw)

      return@autoRelease ResponseInfo(packetType, InMemoryByteSink.fromArray(packetPayloadRaw))
    }
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