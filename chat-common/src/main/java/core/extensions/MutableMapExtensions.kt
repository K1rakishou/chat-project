package core.extensions

fun <T> MutableMap<String, T>.getMany(keys: List<String>): List<T> {
  val resultList = mutableListOf<T>()

  for (key in keys) {
    if (this.containsKey(key)) {
      resultList += this[key]!!
    }
  }

  return resultList
}