package core

import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.isClosed
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.ByteWriteChannel
import java.util.concurrent.atomic.AtomicBoolean

class Connection(
  val clientId: String,
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
      println("Already disposed")
      return
    }

    try {
      if (!socket.isClosed) {
        socket.close()
      }
    } catch (error: Throwable) {
      error.printStackTrace()
      //ignore
    }
  }
}