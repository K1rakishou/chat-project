import tornadofx.Stylesheet
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
  }

  companion object {
    val chatRoomTextArea by cssclass()
    val chatRoomTextField by cssclass()
    val chatRoomViewEmptyLabel by cssclass()
  }
}