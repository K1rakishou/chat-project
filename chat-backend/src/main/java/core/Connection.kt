package core

import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.isClosed
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.ByteWriteChannel
import kotlinx.coroutines.experimental.io.close
import java.util.concurrent.atomic.AtomicBoolean

class Connection(
  val clientAddress: String,
  val socket: Socket,
  val readChannel: ByteReadChannel = socket.openReadChannel(),
  val writeChannel: ByteWriteChannel = socket.openWriteChannel(autoFlush = false)
) {
  private val disposed = AtomicBoolean(false)
  val isDisposed: Boolean
    get() = disposed.get()

  fun dispose() {
    if (!disposed.compareAndSet(false, true)) {
      //already disposed
      return
    }

    try {
      if (!writeChannel.isClosedForWrite) {
        writeChannel.close()
      }
    } catch (error: Throwable) {
      //ignore
    }

    try {
      if (!socket.isClosed) {
        socket.close()
      }
    } catch (error: Throwable) {
      //ignore
    }
  }
}