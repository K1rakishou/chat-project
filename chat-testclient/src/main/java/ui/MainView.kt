package ui

import controller.MainViewController
import tornadofx.*

class MainView : View() {
  val controller: MainViewController by inject()

  override val root = listview(controller.values) {
    useMaxHeight = true
    useMaxWidth = true
  }
}