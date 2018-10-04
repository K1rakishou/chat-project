import org.bouncycastle.jce.provider.BouncyCastleProvider
import tornadofx.launch
import java.security.Security

fun main(args: Array<String>) {
  Security.addProvider(BouncyCastleProvider())

  launch<ChatApp>(args)
}