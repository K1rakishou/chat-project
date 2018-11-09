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