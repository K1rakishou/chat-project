package core

import core.interfaces.CanMeasureSizeOfFields
import java.lang.RuntimeException

inline fun <reified T> sizeof(obj: T? = null): Int {
  return when (T::class) {
    Status::class -> 2
    Boolean::class -> 1
    Byte::class -> 1
    Short::class -> 2
    Int::class -> 4
    Long::class -> 8
    Float::class -> 4
    Double::class -> 8
    String::class -> {
      if (obj == null) {
        1 //1 byte -> NO_VALUE flag
      } else {
        //1 byte -> HAS_VALUE flag
        //4 bytes -> string length
        //the rest is string bytes

        1 + 4 + (obj as String).length
      }
    }
    ByteArray::class -> {
      if (obj == null) {
        1 //1 byte -> NO_VALUE flag
      } else {
        //1 byte -> HAS_VALUE flag
        //4 bytes -> array length
        //the rest is array bytes

        1 + 4 + (obj as ByteArray).size
      }
    }
    else -> throw RuntimeException("Not Implemented for type ${T::class}!")
  }
}

inline fun <reified T : CanMeasureSizeOfFields> sizeofList(objList: List<T>): Int {
  return objList.asSequence().map { it.getSize() }.reduce { acc, i -> acc + i } + 2 //two bytes for list size
}