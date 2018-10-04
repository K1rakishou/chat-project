package core.interfaces

import core.byte_sink.ByteSink

interface CanBeRestoredFromSink {
  fun <T> createFromByteSink(byteSink: ByteSink): T?
}