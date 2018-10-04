package core.model.drainable

import core.byte_sink.ByteSink
import core.interfaces.CanBeDrainedToSink
import java.lang.IllegalStateException
import kotlin.reflect.KClass

object DrainableFactory {

  fun <T> fromByteSink(clazz: KClass<*>, byteSink: ByteSink): T?
    where T : CanBeDrainedToSink {

    return when (clazz) {
      PublicChatRoom::class -> {
        PublicChatRoom.createFromByteSink<PublicChatRoom>(byteSink) as T
      }
      PublicUserInChat::class -> {
        PublicUserInChat.createFromByteSink<PublicUserInChat>(byteSink) as T
      }
      else -> throw IllegalStateException("Not implemented for $clazz")
    }
  }
}