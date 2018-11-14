import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*

class Styles : Stylesheet() {
  private val bgColorDark = c(45, 46, 49)
  private val bgColorBright = c(75, 76, 79)
  private val txtColor = c(196, 198, 197)
  private val accntColorDark = c(0, 128, 128)
  private val accntColorBright = c(0, 168, 168)

  init {
    //connection window
    connectionWindow {
      backgroundColor += bgColorDark

      fieldset {
        field {
          label {
            textFill = txtColor
          }

          textField {
            backgroundColor += bgColorDark
            borderColor += box(accntColorDark)
            textFill = txtColor
            accentColor = accntColorDark

            and(focused) {
              backgroundColor += bgColorBright
            }

            and(hover) {
              borderColor += box(accntColorBright)
            }
          }
        }
      }

      connectButton {
        backgroundColor += accntColorDark
        borderColor += box(txtColor)
        textFill = txtColor

        and(hover) {
          backgroundColor += accntColorBright
          borderColor += box(Color.WHITE)
        }
      }
    }

    chatRoomTextArea {
      fontSize = 18.px
    }
    chatRoomTextField {
      fontSize = 18.px
    }
    chatRoomViewEmptyLabel {
      fontSize = 24.px
    }

    systemMessage {
      fontSize = 20.px
      fontWeight = FontWeight.BOLD
      textFill = c(47, 47, 175)
    }
    senderName {
      fontSize = 20.px
      fontWeight = FontWeight.BOLD
      textFill = c(175, 47, 47)
    }
    receiverName {
      fontSize = 20.px
      fontWeight = FontWeight.BOLD
      textFill = c(47, 175, 47)
    }

    myTextChatMessage {
      fontSize = 16.px
      textFill = c(172, 172, 172)
    }
    myTextChatMessageAcceptedByServer {
      fontSize = 16.px
      textFill = c(0, 0, 0)
    }
    systemTextChatMessage {
      fontSize = 16.px
      textFill = c(0, 0, 0)
    }
    foreignTextChatMessage {
      fontSize = 16.px
      textFill = c(0, 0, 0)
    }
  }

  companion object {
    //connection window
    val connectionWindow by cssclass()
    val connectButton by cssclass()

    val chatRoomTextArea by cssclass()
    val chatRoomTextField by cssclass()
    val chatRoomViewEmptyLabel by cssclass()

    val systemMessage by cssclass()
    val senderName by cssclass()
    val receiverName by cssclass()

    val myTextChatMessage by cssclass()
    val myTextChatMessageAcceptedByServer by cssclass()
    val systemTextChatMessage by cssclass()
    val foreignTextChatMessage by cssclass()
  }
}