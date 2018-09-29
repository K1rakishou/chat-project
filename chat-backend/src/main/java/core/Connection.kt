package core

import kotlinx.coroutines.experimental.io.ByteWriteChannel

class Connection(
  val clientAddress: String,
  val writeChannel: ByteWriteChannel
)