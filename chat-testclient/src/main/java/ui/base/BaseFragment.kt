package ui.base

import javafx.scene.control.Alert
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import tornadofx.FXEventRegistration
import tornadofx.Fragment
import tornadofx.alert
import kotlin.coroutines.CoroutineContext

abstract class BaseFragment(
  title: String? = null
) : Fragment(title), IView, CoroutineScope {

  protected val job = Job()
  protected val fxEventList = mutableListOf<FXEventRegistration>()
  private val bgThread = newFixedThreadPoolContext(1, "bg-fragment")

  override val coroutineContext: CoroutineContext
    get() = job

  override fun onDock() {
    super.onDock()
  }

  override fun onUndock() {
    job.cancel()

    fxEventList.forEach { it.unsubscribe() }
    fxEventList.clear()

    super.onUndock()
  }

  protected fun doOnUI(block: suspend () -> Unit) {
    launch(coroutineContext + Dispatchers.JavaFx) {
      block()
    }
  }

  protected fun doOnBg(block: suspend () -> Unit) {
    launch(coroutineContext + bgThread) {
      block()
    }
  }

  protected fun showErrorAlert(message: String) {
    doOnUI {
      alert(Alert.AlertType.ERROR, "Error", message)
    }
  }

  protected fun FXEventRegistration.autoUnsubscribe() {
    fxEventList.add(this)
  }
}