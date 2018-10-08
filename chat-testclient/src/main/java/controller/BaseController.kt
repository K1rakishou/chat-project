package controller

import javafx.scene.control.Alert
import tornadofx.Controller
import tornadofx.alert
import tornadofx.runLater

abstract class BaseController : Controller() {

  protected fun showErrorAlert(message: String) {
    runLater {
      alert(Alert.AlertType.ERROR, "Error", message)
    }
  }

}