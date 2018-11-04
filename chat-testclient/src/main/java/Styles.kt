import javafx.scene.text.FontWeight
import tornadofx.Stylesheet
import tornadofx.c
import tornadofx.cssclass
import tornadofx.px

class Styles : Stylesheet() {
  init {
    chatRoomTextArea {
      fontSize = 18.px
    }
    chatRoomTextField {
      fontSize = 18.px
    }
    chatRoomViewEmptyLabel {
      fontSize = 24.px
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
    textChatMessage {
      fontSize = 16.px
    }
  }

  companion object {
    val chatRoomTextArea by cssclass()
    val chatRoomTextField by cssclass()
    val chatRoomViewEmptyLabel by cssclass()

    val senderName by cssclass()
    val receiverName by cssclass()
    val textChatMessage by cssclass()
  }
}