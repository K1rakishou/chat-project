package manager

import java.util.concurrent.atomic.AtomicInteger

object IdGeneratorManager {
  private val clientMessageIdGenerator = AtomicInteger(0)

  fun getNextClientMessageId(): Int {
    return clientMessageIdGenerator.getAndIncrement()
  }
}