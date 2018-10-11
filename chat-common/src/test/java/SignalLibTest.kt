import core.byte_sink.ByteSink
import core.byte_sink.InMemoryByteSink
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.groups.GroupCipher
import org.whispersystems.libsignal.groups.GroupSessionBuilder
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.groups.state.SenderKeyRecord
import org.whispersystems.libsignal.groups.state.SenderKeyStore
import org.whispersystems.libsignal.protocol.SenderKeyDistributionMessage
import java.io.IOException
import org.whispersystems.libsignal.util.KeyHelper
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.IdentityKeyPair
import org.whispersystems.libsignal.protocol.CiphertextMessage
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.SessionBuilder
import org.whispersystems.libsignal.state.IdentityKeyStore
import org.whispersystems.libsignal.state.SignedPreKeyStore
import org.whispersystems.libsignal.state.PreKeyStore
import org.whispersystems.libsignal.state.SessionStore
import org.whispersystems.libsignal.state.impl.InMemoryIdentityKeyStore
import org.whispersystems.libsignal.state.impl.InMemoryPreKeyStore
import org.whispersystems.libsignal.state.impl.InMemorySessionStore
import org.whispersystems.libsignal.state.impl.InMemorySignedPreKeyStore


class SignalLibTest {

  private val SENDER_ADDRESS = SignalProtocolAddress("test_group", 1)
  private val GROUP_SENDER = SenderKeyName("nihilist history reading group", SENDER_ADDRESS)

  class InMemorySenderKeyStore : SenderKeyStore {
    private val store = mutableMapOf<SenderKeyName, SenderKeyRecord>()

    override fun loadSenderKey(senderKeyName: SenderKeyName): SenderKeyRecord {
      try {
        val record = store[senderKeyName]

        return if (record == null) {
          SenderKeyRecord()
        } else {
          SenderKeyRecord(record.serialize())
        }
      } catch (e: IOException) {
        throw AssertionError(e)
      }
    }

    override fun storeSenderKey(senderKeyName: SenderKeyName, senderKeyRecord: SenderKeyRecord) {
      store[senderKeyName] = senderKeyRecord
    }

  }

  private fun serialize(distributionMessage: SenderKeyDistributionMessage, cipherText: ByteArray): ByteSink {
    val byteSink = InMemoryByteSink.createWithInitialSize(512)
    byteSink.writeByteArray(distributionMessage.serialize())
    byteSink.writeByteArray(cipherText)

    return byteSink
  }

  private fun decrypt(byteSink: ByteSink, sessionBuilder: GroupSessionBuilder, cipher: GroupCipher): ByteArray {
    val distributionMessage = byteSink.readByteArray(500)
    val cipherText = byteSink.readByteArray(500)

    sessionBuilder.process(GROUP_SENDER, SenderKeyDistributionMessage(distributionMessage))
    return cipher.decrypt(cipherText)
  }

  @Test
  fun test() {
    //FIXME: does not work
    val aliceStore = InMemorySenderKeyStore()
    val bobStore = InMemorySenderKeyStore()
    val charleyStore = InMemorySenderKeyStore()
    val johnStore = InMemorySenderKeyStore()

    val aliceSessionBuilder = GroupSessionBuilder(aliceStore)
    val bobSessionBuilder = GroupSessionBuilder(bobStore)
    val charleySessionBuilder = GroupSessionBuilder(charleyStore)
    val johnSessionBuilder = GroupSessionBuilder(johnStore)

    val aliceGroupCipher = GroupCipher(aliceStore, GROUP_SENDER)
    val bobGroupCipher = GroupCipher(bobStore, GROUP_SENDER)
    val charleyGroupCipher = GroupCipher(charleyStore, GROUP_SENDER)
    val johnGroupCipher = GroupCipher(johnStore, GROUP_SENDER)

    kotlin.run {
      val sentAliceDistributionMessage = aliceSessionBuilder.create(GROUP_SENDER)
      val receivedAliceDistributionMessage = SenderKeyDistributionMessage(sentAliceDistributionMessage.serialize())

      val originalText = "test message 1"
      val cipherTextFromAlice = aliceGroupCipher.encrypt(originalText.toByteArray())

      val bobByteSink = serialize(receivedAliceDistributionMessage, cipherTextFromAlice)
      val charleyByteSink = serialize(receivedAliceDistributionMessage, cipherTextFromAlice)
      val johnByteSink = serialize(receivedAliceDistributionMessage, cipherTextFromAlice)

      assertArrayEquals(originalText.toByteArray(), decrypt(bobByteSink, bobSessionBuilder, bobGroupCipher))
      assertArrayEquals(originalText.toByteArray(), decrypt(charleyByteSink, charleySessionBuilder, charleyGroupCipher))
      assertArrayEquals(originalText.toByteArray(), decrypt(johnByteSink, johnSessionBuilder, johnGroupCipher))
    }

    kotlin.run {
      val sentBobDistributionMessage = bobSessionBuilder.create(GROUP_SENDER)
      val receivedBobDistributionMessage = SenderKeyDistributionMessage(sentBobDistributionMessage.serialize())

      val originalText = "test message 1"
      val cipherTextFromBob = bobGroupCipher.encrypt(originalText.toByteArray())

      val aliceByteSink = serialize(receivedBobDistributionMessage, cipherTextFromBob)
      val charleyByteSink = serialize(receivedBobDistributionMessage, cipherTextFromBob)
      val johnByteSink = serialize(receivedBobDistributionMessage, cipherTextFromBob)

      assertArrayEquals(originalText.toByteArray(), decrypt(aliceByteSink, aliceSessionBuilder, aliceGroupCipher))
      assertArrayEquals(originalText.toByteArray(), decrypt(charleyByteSink, charleySessionBuilder, charleyGroupCipher))
      assertArrayEquals(originalText.toByteArray(), decrypt(johnByteSink, johnSessionBuilder, johnGroupCipher))
    }
  }

  fun test2() {
    val aliceIdentityKeyPair = KeyHelper.generateIdentityKeyPair()
    val aliceRegistrationId = KeyHelper.generateRegistrationId(true)
    val alicePreKeys = KeyHelper.generatePreKeys(0, 10)
    val aliceSignedPreKey = KeyHelper.generateSignedPreKey(aliceIdentityKeyPair, 5)
    val aliceSessionStore = InMemorySessionStore()
    val alicePreKeyStore = InMemoryPreKeyStore()
    val aliceSignedPreKeyStore = InMemorySignedPreKeyStore()
    val aliceIdentityStore = InMemoryIdentityKeyStore(aliceIdentityKeyPair, aliceRegistrationId)

    val bobIdentityKeyPair = KeyHelper.generateIdentityKeyPair()
    val bobRegistrationId = KeyHelper.generateRegistrationId(true)
    val bobPreKeys = KeyHelper.generatePreKeys(0, 10)
    val bobSignedPreKey = KeyHelper.generateSignedPreKey(bobIdentityKeyPair, 5)
    val bobSessionStore = InMemorySessionStore()
    val bobPreKeyStore = InMemoryPreKeyStore()
    val bobSignedPreKeyStore = InMemorySignedPreKeyStore()
    val bobIdentityStore = InMemoryIdentityKeyStore(bobIdentityKeyPair, bobRegistrationId)


//    val sessionBuilder = SessionBuilder(sessionStore, preKeyStore, signedPreKeyStore,
//      identityStore, SignalProtocolAddress(SENDER_ADDRESS.name, SENDER_ADDRESS.deviceId))
//
//    sessionBuilder.process(retrievedPreKey)
//
//    val sessionCipher = SessionCipher(sessionStore, recipientId, deviceId)
//    val message = sessionCipher.encrypt("Hello world!".toByteArray(charset("UTF-8")))
  }
}