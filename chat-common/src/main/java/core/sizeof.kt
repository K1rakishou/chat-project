package core

import java.lang.RuntimeException

inline fun <reified T> sizeof(obj: T? = null): Int {
  return when (T::class) {
    Byte::class -> 1
    Short::class -> 2
    Int::class -> 4
    Long::class -> 8
    Float::class -> 4
    Double::class -> 8
    String::class -> 4 + (obj as String).length
    else -> throw RuntimeException("Not Implemented!")
  }
}