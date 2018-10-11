package core.extensions

import java.io.DataInputStream

fun DataInputStream.forEachChunk(_offset: Int, chunkSize: Int, totalSize: Int, block: (ByteArray) -> Unit) {
  use { dis ->
    for (currentIndex in _offset until totalSize step chunkSize) {
      val currentChunkSize = if (totalSize - currentIndex > chunkSize) {
        chunkSize
      } else {
        totalSize - currentIndex
      }

      val array = ByteArray(currentChunkSize)
      dis.readFully(array)

      block(array)
    }
  }
}

suspend fun DataInputStream.forEachChunkAsync(_offset: Int, chunkSize: Int, totalSize: Int, block: suspend (ByteArray) -> Unit) {
  use { dis ->
    for (currentIndex in _offset until totalSize step chunkSize) {
      val currentChunkSize = if (totalSize - currentIndex > chunkSize) {
        chunkSize
      } else {
        totalSize - currentIndex
      }

      val array = ByteArray(currentChunkSize)
      dis.readFully(array)

      block(array)
    }
  }
}