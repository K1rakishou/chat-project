package extensions

import core.Constants
import core.ResponseInfo
import core.ResponseType
import core.byte_sink.InMemoryByteSink
import core.byte_sink.OnDiskByteSink
import core.utils.TimeUtils
import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.readAvailable
import kotlinx.io.core.IoBuffer
import kotlinx.io.core.readFully
import java.io.File
import java.io.RandomAccessFile

suspend fun ByteReadChannel.readResponseInfo(byteSinkFileCachePath: String, bodySize: Int): ResponseInfo {
  var responseInfo: ResponseInfo? = null

  if (bodySize <= Constants.MAX_PACKET_SIZE_FOR_MEMORY_HANDLING) {
    IoBuffer.Pool.autoRelease { buffer ->
      this.readFully(buffer, bodySize)

      val packetId = buffer.readLong()
      val packetType = ResponseType.fromShort(buffer.readShort())

      val packetPayloadRaw = ByteArray(buffer.readRemaining)
      buffer.readFully(packetPayloadRaw)

      responseInfo = ResponseInfo(packetId, packetType, InMemoryByteSink.fromArray(packetPayloadRaw))
    }
  } else {
    val file = File("$byteSinkFileCachePath\\test_file-${TimeUtils.getCurrentTime()}.tmp")
    val randomAccessFile = RandomAccessFile(file, "w")

    randomAccessFile.use { raf ->
      val sink = OnDiskByteSink.fromFile(file)

      for (offset in 0 until bodySize step Constants.MAX_PACKET_SIZE_FOR_MEMORY_HANDLING) {
        val chunk = if (bodySize - offset > Constants.MAX_PACKET_SIZE_FOR_MEMORY_HANDLING) {
          Constants.MAX_PACKET_SIZE_FOR_MEMORY_HANDLING
        } else {
          bodySize - offset
        }

        val array = ByteArray(chunk)
        this.readAvailable(array)
        raf.write(array)
      }

      val packetId = sink.readLong()
      val packetType = ResponseType.fromShort(sink.readShort())

      responseInfo = ResponseInfo(packetId, packetType, sink)
    }
  }

  return responseInfo!!
}