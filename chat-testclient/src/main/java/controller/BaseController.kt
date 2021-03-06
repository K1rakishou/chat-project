package controller

import io.reactivex.disposables.CompositeDisposable
import javafx.scene.control.Alert
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import tornadofx.Controller
import tornadofx.alert
import ui.base.IView
import kotlin.coroutines.CoroutineContext

abstract class BaseController<View: IView> : Controller(), CoroutineScope {

  protected lateinit var view: View
  protected lateinit var job: Job
  protected lateinit var compositeDisposable: CompositeDisposable
  private val bgThread = newFixedThreadPoolContext(1, "bg-controller")

  override val coroutineContext: CoroutineContext
    get() = job

  open fun createController(viewParam: View) {
    view = viewParam
    job = Job()
    compositeDisposable = CompositeDisposable()
  }

  open fun destroyController() {
    job.cancel()
    compositeDisposable.dispose()
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
    doOnBg {
      alert(Alert.AlertType.ERROR, "Error", message)
    }
  }

}