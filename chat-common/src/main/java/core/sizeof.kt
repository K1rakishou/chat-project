package core

import core.interfaces.CanMeasureSizeOfFields
import core.model.drainable.ChatRoomData
import core.model.drainable.PublicUserInChat
import core.model.drainable.chat_message.TextChatMessage

inline fun <reified T> sizeof(obj: T? = null): Int {
  return when (T::class) {
    PublicUserInChat::class -> (obj as? PublicUserInChat)?.getSize()?.plus(1) ?: 1
    ChatRoomData::class -> (obj as? ChatRoomData)?.getSize()?.plus(1) ?: 1
    TextChatMessage::class -> (obj as? TextChatMessage)?.getSize()?.plus(1) ?: 1
    Status::class -> 2
    Boolean::class -> 1
    Byte::class -> 1
    Short::class -> 2
    Int::class -> 4
    Long::class -> 8
    Float::class -> 4
    Double::class -> 8
    String::class -> {
      if (obj == null || (obj as String).isEmpty()) {
        1 //1 byte -> NO_VALUE flag
      } else {
        //1 byte -> HAS_VALUE flag
        //4 bytes -> string length
        //the rest is string bytes

        1 + 4 + (obj as String).length
      }
    }
    ByteArray::class -> {
      if (obj == null || (obj as ByteArray).isEmpty()) {
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

inline fun <reified T : CanMeasureSizeOfFields> sizeofList(objList: List<T>?): Int {
  if (objList == null || objList.isEmpty()) {
    return 1  //NO_VALUE flag
  } else {
    return objList.asSequence().map { it.getSize() }.reduce { acc, i -> acc + i } + 2 + 1 //two bytes for list size + one byte HAS_VALUE flag
  }
}