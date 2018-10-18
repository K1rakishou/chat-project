package ui.base

import javafx.scene.control.Alert
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import tornadofx.FXEventRegistration
import tornadofx.View
import tornadofx.alert
import kotlin.coroutines.experimental.CoroutineContext

abstract class BaseView(
  title: String? = null
) : View(title), IView, CoroutineScope {

  protected val job = Job()
  protected val fxEventList = mutableListOf<FXEventRegistration>()

  override val coroutineContext: CoroutineContext
    get() = job
  override val isActive: Boolean
    get() = job.isActive

  override fun onDock() {
    super.onDock()
  }

  override fun onUndock() {
    job.cancel()

    fxEventList.forEach { it.unsubscribe() }
    fxEventList.clear()

    super.onUndock()
  }

  protected fun showErrorAlert(message: String) {
    launch(coroutineContext + JavaFx) {
      alert(Alert.AlertType.ERROR, "Error", message)
    }
  }

  protected fun FXEventRegistration.autoUnsubscribe() {
    fxEventList.add(this)
  }
}