package core.collections

/**
 * Thread-unsafe implementation of the RingBuffer collection
 * */
class RingBuffer<T>(
  val size: Int
) {
  @Suppress("UNCHECKED_CAST")
  private val array = arrayOfNulls<Any?>(size) as Array<T>

  var emptyCount = size
    private set

  var headIndex = 0
    private set
  fun add(element: T) {
    array[headIndex] = element

    headIndex = (headIndex + 1) % size
    if (emptyCount - 1 >= 0) {
      --emptyCount
    }
  }

  fun get(index: Int): T? {
    check(index >= 0)
    check(index <= array.size)

    return array[index]
  }

  @Suppress("UNCHECKED_CAST")
  fun getAll(): List<T> {
    check(size >= emptyCount)

    val count = size - emptyCount

    //Have no idea how to do it with plain arrays
    //Kotlin is pretty bad when you want to create an array with initial size
    val list = ArrayList<T>(count)
    val start = headIndex - count

    for (i in start until headIndex) {
      val index = if (i < 0) {
        i + count
      } else {
        i
      }

      list.add(array[index])
    }

    return list
  }
}