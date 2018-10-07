package core.exception

import java.lang.Exception

class MaxListSizeExceededException(
  listSize: Int,
  maxListSize: Int
) : Exception("Exceeded max list size (listSize: $listSize, maxListSize: $maxListSize)")