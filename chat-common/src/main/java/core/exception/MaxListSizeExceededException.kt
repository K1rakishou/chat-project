package core.exception

import java.lang.Exception

class MaxListSizeExceededException(listSize: Short, maxListSize: Int) : Exception("Exceeded max list size (listSize: $listSize, maxListSize: $maxListSize)")