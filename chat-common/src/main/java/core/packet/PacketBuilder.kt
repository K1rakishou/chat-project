package core.packet

import core.Constants
import core.Packet
import core.byte_sink.ByteSink
import core.byte_sink.InMemoryByteSink
import core.security.SecurityUtils
import core.sizeof
import org.bouncycastle.crypto.params.AsymmetricKeyParameter

class PacketBuilder {

  fun buildUnencryptedPacket(packet: UnencryptedPacket, byteSink: ByteSink): Packet? {
    val totalBodySize = (Packet.PACKET_BODY_SIZE + packet.getPayloadSize())
    if (totalBodySize > Int.MAX_VALUE) {
      println("bodySize exceeds Int.MAX_VALUE: $totalBodySize")
      return null
    }

    packet.toByteSink(byteSink)

    val packetBody = Packet.PacketBody(
      packet.getPacketType().value,
      byteSink
    )

    return Packet(
      Packet.MAGIC_NUMBER,
      totalBodySize,
      packetBody
    )
  }

  //byteSink should be closed by the caller
  fun buildEncryptedPacket(
    ecPrivateKey: AsymmetricKeyParameter,
    sharedSecret: ByteArray,
    packet: EncryptedPacket,
    byteSink: ByteSink
  ): Packet? {
    val totalBodySize = (Packet.PACKET_BODY_SIZE + packet.getPayloadSize())
    if (totalBodySize > Int.MAX_VALUE) {
      println("bodySize exceeds Int.MAX_VALUE: $totalBodySize")
      return null
    }

    val iv = SecurityUtils.Generator.generateRandomByteArray(Constants.ivLen)
    val randomBytes = SecurityUtils.Generator.generateRandomByteArray(Constants.randomBytesLen)
    SecurityUtils.Encryption.xSalsa20Encrypt(sharedSecret, iv, byteSink, byteSink.getWriterPosition())

    //TODO: remove getArray, it may cause OOM
    val signature = SecurityUtils.Signing.generateSignature(ecPrivateKey, byteSink.getArray())
    if (signature == null) {
      println("Could not generate signature for packet body")
      return null
    }

    val byteSinkSize = sizeof(randomBytes) + sizeof(iv) + sizeof(signature) + byteSink.getWriterPosition()
    val encryptedByteSink = InMemoryByteSink.createWithInitialSize(byteSinkSize)

    encryptedByteSink.writeByteArray(randomBytes)
    encryptedByteSink.writeByteArray(iv)
    encryptedByteSink.writeByteArray(signature)
    encryptedByteSink.writeByteSink(byteSink)

    val packetBody = Packet.PacketBody(
      packet.getPacketType().value,
      encryptedByteSink
    )

    return Packet(
      Packet.MAGIC_NUMBER,
      byteSink.getWriterPosition(),
      packetBody
    )
  }
}