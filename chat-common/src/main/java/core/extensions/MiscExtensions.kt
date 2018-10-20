package core.extensions

import kotlinx.io.core.IoBuffer
import kotlinx.io.pool.ObjectPool


suspend fun <T> ObjectPool<IoBuffer>.autoRelease(block: suspend (IoBuffer) -> T): T {
  val buffer = this.borrow()

  try {
    return block(buffer)
  } finally {
    buffer.release(this)
  }
}