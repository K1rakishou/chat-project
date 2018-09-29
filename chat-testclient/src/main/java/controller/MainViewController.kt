package controller

import javafx.collections.FXCollections
import tornadofx.Controller

class MainViewController : Controller() {
  val values = FXCollections.observableArrayList("Alpha","Beta","Gamma","Delta")
}