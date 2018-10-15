package core.extensions

import kotlinx.coroutines.experimental.sync.Mutex

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