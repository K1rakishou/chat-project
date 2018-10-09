package core.packet

import core.Packet
import core.byte_sink.ByteSink
import core.byte_sink.InMemoryByteSink
import core.security.SecurityUtils
import core.sizeof
import org.bouncycastle.crypto.params.AsymmetricKeyParameter

object PacketBuilder {
  const val ivLen = 24
  const val randomBytesLen = 16

//  fun buildPacket(packet: BasePacket, byteSink: ByteSink = InMemoryByteSink.createWithInitialSize(packet.getPayloadSize())): Packet {
//    return when (packet) {
//      is UnencryptedPacket -> buildUnencryptedPacket(packet, byteSink)
//      is EncryptedPacket -> buildEncryptedPacket(packet, byteSink)
//      else -> throw IllegalStateException("Not implemented from ${packet::class}")
//    }
//  }

  fun buildUnencryptedPacket(packet: UnencryptedPacket, byteSink: ByteSink): Packet {
    val totalBodySize = (Packet.PACKET_BODY_SIZE + packet.getPayloadSize())
    if (totalBodySize > Int.MAX_VALUE) {
      throw RuntimeException("bodySize exceeds Int.MAX_VALUE: $totalBodySize")
    }

    packet.toByteSink(byteSink)

    val packetBody = Packet.PacketBody(
      //TODO: REMOVE
      -1L,
      packet.getPacketType().value,
      byteSink
    )

    return Packet(
      Packet.MAGIC_NUMBER,
      totalBodySize,
      packetBody
    )
  }

  fun buildEncryptedPacket(
    ecPrivateKey: AsymmetricKeyParameter,
    sharedSecret: ByteArray,
    packet: EncryptedPacket,
    byteSink: ByteSink
  ): Packet {
    try {
      val totalBodySize = (Packet.PACKET_BODY_SIZE + packet.getPayloadSize())
      if (totalBodySize > Int.MAX_VALUE) {
        throw RuntimeException("bodySize exceeds Int.MAX_VALUE: $totalBodySize")
      }

      val iv = SecurityUtils.Generator.generateRandomByteArray(PacketBuilder.ivLen)
      val randomBytes = SecurityUtils.Generator.generateRandomByteArray(PacketBuilder.randomBytesLen)
      SecurityUtils.Encryption.xSalsa20Encrypt(sharedSecret, iv, byteSink, byteSink.getWriterPosition())

      //TODO: remove getArray, it may cause OOM
      val signature = SecurityUtils.Signing.generateSignature(ecPrivateKey, byteSink.getArray())
      if (signature == null) {
        throw RuntimeException("Could not generate signature for packet body")
      }

      val byteSinkSize = sizeof(randomBytes) + sizeof(iv) + sizeof(signature) + byteSink.getWriterPosition()
      val encryptedByteSink = InMemoryByteSink.createWithInitialSize(byteSinkSize)

      encryptedByteSink.writeByteArray(randomBytes)
      encryptedByteSink.writeByteArray(iv)
      encryptedByteSink.writeByteArray(signature)

      //TODO: remove readByteArrayRaw, it may cause OOM
      encryptedByteSink.writeByteArrayRaw(
        encryptedByteSink.getWriterPosition(),
        byteSink.readByteArrayRaw(0, byteSink.getWriterPosition())
      )

      val packetBody = Packet.PacketBody(
        //TODO: REMOVE
        -1L,
        packet.getPacketType().value,
        encryptedByteSink
      )

      return Packet(
        Packet.MAGIC_NUMBER,
        byteSink.getWriterPosition(),
        packetBody
      )

    } finally {
      byteSink.close()
    }
  }
}