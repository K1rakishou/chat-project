package core.extensions

import kotlinx.coroutines.sync.Mutex
import java.lang.IllegalStateException

/**
 * If mutex is already locked - do not attempt to lock it again
 * */
suspend fun <T> Mutex.tryWithLock(block: suspend () -> T): T {
  if (!tryLock()) {
    return block()
  }

  try {
    return block()
  } finally {
    unlock()
  }
}

/**
 * If mutex is already locked - throw an exception that we got deadlocked
 * */
suspend fun <T> Mutex.myWithLock(block: suspend () -> T): T {
  if (!tryLock()) {
    throw IllegalStateException("DEADLOCK")
  }

  try {
    return block()
  } finally {
    unlock()
  }
}