package controller

import io.reactivex.disposables.CompositeDisposable
import javafx.scene.control.Alert
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import tornadofx.Controller
import tornadofx.alert
import tornadofx.runLater
import ui.base.IView
import kotlin.coroutines.experimental.CoroutineContext

abstract class BaseController<View: IView> : Controller(), CoroutineScope {

  protected lateinit var view: View
  protected lateinit var job: Job
  protected lateinit var compositeDisposable: CompositeDisposable

  override val coroutineContext: CoroutineContext
    get() = job
  override val isActive: Boolean
    get() = job.isActive

  open fun createController(viewParam: View) {
    view = viewParam
    job = Job()
    compositeDisposable = CompositeDisposable()
  }

  open fun destroyController() {
    job.cancel()
    compositeDisposable.dispose()
  }

  protected fun showErrorAlert(message: String) {
    runLater {
      alert(Alert.AlertType.ERROR, "Error", message)
    }
  }

}