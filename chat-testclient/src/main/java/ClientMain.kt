import org.bouncycastle.jce.provider.BouncyCastleProvider
import tornadofx.launch
import ui.ChatApp
import java.security.Security

fun main(args: Array<String>) {
  Security.setProperty("crypto.policy", "unlimited")
  Security.addProvider(BouncyCastleProvider())

  launch<ChatApp>(args)
}