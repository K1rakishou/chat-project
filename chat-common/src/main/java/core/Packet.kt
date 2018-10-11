package core

import core.byte_sink.ByteSink

class Packet(
  val magicNumber: Int,       //4
  val bodySize: Int,          //4
  val type: Short,            //2
  val bodyByteSink: ByteSink
) {

  companion object {
    val MAGIC_NUMBER_BYTES = arrayOf<Byte>(0x44, 0x45, 0x53, 0x55)

    const val MAGIC_NUMBER = 0x44455355
    const val PACKET_BODY_SIZE = 2
  }
}