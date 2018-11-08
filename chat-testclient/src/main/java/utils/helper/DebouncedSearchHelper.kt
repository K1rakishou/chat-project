package utils.helper

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class DebouncedSearchHelper {
  private val debounceTime = 500L
  private val subject = PublishSubject.create<String>()

  private var callback: ((String) -> Unit)? = null
  private lateinit var compositeDisposable: CompositeDisposable

  fun start(_callback: (String) -> Unit) {
    callback = _callback

    compositeDisposable = CompositeDisposable()
    compositeDisposable += subject
      .debounce(debounceTime, TimeUnit.MILLISECONDS)
      .subscribe({ text -> callback?.invoke(text) }, { error -> error.printStackTrace() })
  }

  fun stop() {
    callback = null
    compositeDisposable.dispose()
  }

  fun process(text: String) {
    subject.onNext(text)
  }
}