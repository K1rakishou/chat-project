package core.exception

import java.lang.Exception

class ByteSinkBufferOverflowException(
  arrayLen: Int,
  maxSize: Int
) : Exception("Buffer overflow! arrayLen exceeds maxSize ($arrayLen > $maxSize)")