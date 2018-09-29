package core

fun ByteArray.getShort(offset: Int): Short {
  var result: Int = 0

  for (i in 0..1) {
    result = result shl 8
    result = result or (this[offset + i].toInt() and 0xFF)
  }

  return result.toShort()
}

fun ByteArray.getInt(offset: Int): Int {
  var result: Int = 0

  for (i in 0..3) {
    result = result shl 8
    result = result or (this[offset + i].toInt() and 0xFF)
  }

  return result
}

fun ByteArray.getLong(offset: Int): Long {
  var result: Long = 0

  for (i in 0..7) {
    result = result shl 8
    result = result or (this[offset + i].toLong() and 0xFF)
  }

  return result
}

private val HEX_CHARS = "0123456789ABCDEF".toCharArray()
fun ByteArray.toHex() : String{
  val result = StringBuffer()

  forEach {
    val octet = it.toInt()
    val firstIndex = (octet and 0xF0).ushr(4)
    val secondIndex = octet and 0x0F
    result.append(HEX_CHARS[firstIndex])
    result.append(HEX_CHARS[secondIndex])
  }

  return result.toString()
}

fun <T> MutableMap<String, T>.getMany(keys: List<String>): List<T> {
  val resultList = mutableListOf<T>()

  for (key in keys) {
    if (this.containsKey(key)) {
      resultList += this[key]!!
    }
  }

  return resultList
}