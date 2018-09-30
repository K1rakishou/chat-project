import javafx.geometry.Pos
import tornadofx.*

class Styles : Stylesheet() {
  init {
    title {
      fontSize = 3.em
      textFill = c(175, 47, 47, 0.5)
    }
    header {
      alignment = Pos.CENTER
      star {
        alignment = Pos.CENTER_LEFT
      }
    }
  }

  companion object {
    val title by cssid()
    val header by cssclass()
  }
}