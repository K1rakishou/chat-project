package core.exception

import java.lang.Exception

class UnknownPacketVersionException(
  packetVersion: Short
) : Exception("Unknown packet version ($packetVersion)")