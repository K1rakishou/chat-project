import tornadofx.*

class Styles : Stylesheet() {
  init {
//    title {
//      fontSize = 3.em
//      textFill = c(175, 47, 47, 0.5)
//    }
//    header {
//      alignment = Pos.CENTER
//      star {
//        alignment = Pos.CENTER_LEFT
//      }
//    }
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
//    val title by cssid()
//    val header by cssclass()
    val chatRoomTextArea by cssclass()
    val chatRoomTextField by cssclass()
    val chatRoomViewEmptyLabel by cssclass()
  }
}