package core.extensions

private val HEX_CHARS = "0123456789ABCDEF".toCharArray()
fun ByteArray.toHex(separator: Char? = null): String {
  val result = StringBuffer()

  forEach {
    val octet = it.toInt()
    val firstIndex = (octet and 0xF0).ushr(4)
    val secondIndex = octet and 0x0F
    result.append(HEX_CHARS[firstIndex])
    result.append(HEX_CHARS[secondIndex])

    if (separator != null) {
      result.append(separator)
    }
  }

  return result.toString()
}

fun ByteArray.toHexSeparated(separator: Char? = ' '): String {
  return toHex(separator)
}

fun ByteArray?.isNullOrEmpty(): Boolean {
  return this == null || this.isEmpty()
}