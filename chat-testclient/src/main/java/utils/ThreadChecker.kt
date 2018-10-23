package utils

import java.lang.RuntimeException

object ThreadChecker {
  const val mainThreadName = "JavaFX Application Thread"

  fun throwIfNotOnMainThread() {
    val currentThreadName = Thread.currentThread().name
    if (currentThreadName != mainThreadName) {
      throw RuntimeException("This operation must run only be performed main thread! Current thread name = \"$currentThreadName\"")
    }
  }

  fun throwIfOnMainThread() {
    val currentThreadName = Thread.currentThread().name
    if (currentThreadName == mainThreadName) {
      throw RuntimeException("This operation must not be performed on main thread!")
    }
  }

}