package core.interfaces

import core.byte_sink.ByteSink

interface CanBeDrainedToSink {
  fun serialize(sink: ByteSink)
  fun <T> deserialize(sink: ByteSink): T
}