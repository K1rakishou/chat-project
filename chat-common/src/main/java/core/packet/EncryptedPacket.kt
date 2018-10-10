package core.packet

abstract class EncryptedPacket : BasePacket() {
  lateinit var receiverId: String
}