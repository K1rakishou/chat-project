package ui.loading_window

import tornadofx.View
import tornadofx.gridpane
import tornadofx.progressindicator

class LoadingWindow : View() {



  override val root = gridpane {

    progressindicator {

    }
  }

}