package extensions

import kotlinx.coroutines.experimental.sync.Mutex
import java.lang.IllegalStateException

/**
 * If mutex is already locked - do not attempt to lock it again
 * */
suspend fun <T> Mutex.tryWithLock(block: suspend () -> T): T {
  if (isLocked) {
    return block()
  }

  try {
    lock()
    return block()
  } finally {
    unlock()
  }
}

suspend fun <T> Mutex.myWithLock(block: suspend () -> T): T {
  if (isLocked) {
    throw IllegalStateException("DEADLOCK")
  }

  try {
    lock()
    return block()
  } finally {
    unlock()
  }
}