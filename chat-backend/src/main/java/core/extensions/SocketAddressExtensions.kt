package core.extensions

import java.net.SocketAddress

fun SocketAddress.extractIpAddress(isDebug: Boolean): String {
  if (isDebug) {
    return toString()
  }

  val str = toString()
  val startIndex = if (str[0] == '/') {
    1
  } else {
    0
  }

  val endIndex = str.indexOf(':')
  val resultString = str.substring(startIndex, endIndex)

  return resultString
}