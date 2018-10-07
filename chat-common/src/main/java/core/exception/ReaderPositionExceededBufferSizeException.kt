package core.exception

import java.lang.Exception

class ReaderPositionExceededBufferSizeException : Exception {

  constructor(
    readerPosition: Int,
    arrayLen: Int,
    innerBufferSize: Long
  ) : super("ReaderPosition + arrayLen ($readerPosition + $arrayLen) exceeds innerBufferSize ($readerPosition + $arrayLen > $innerBufferSize)")

  constructor(
    readerPosition: Int,
    arrayLen: Int,
    innerBufferSize: Int
  ) : super("ReaderPosition + arrayLen ($readerPosition + $arrayLen) exceeds innerBufferSize ($readerPosition + $arrayLen > $innerBufferSize)")
}