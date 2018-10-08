package core.exception

import kotlin.reflect.KClass

class DrainableDeserializationException(
  clazz: KClass<*>,
  index: Int
) : Exception("List element that's not supposed to be null is null. Class: ${clazz}, elementIndex = ${index}")