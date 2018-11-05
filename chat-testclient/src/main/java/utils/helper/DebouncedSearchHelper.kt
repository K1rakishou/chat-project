package utils.helper

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class DebouncedSearchHelper {
  private val subject = PublishSubject.create<String>()
  private lateinit var callback: WeakReference<(String) -> Unit>
  private lateinit var compositeDisposable: CompositeDisposable

  fun start(_callback: (String) -> Unit) {
    callback = WeakReference(_callback)

    compositeDisposable = CompositeDisposable()
    compositeDisposable += subject
      .debounce(500, TimeUnit.MILLISECONDS)
      .subscribe({ text -> callback.get()?.invoke(text) }, { error -> error.printStackTrace() })
  }

  fun stop() {
    callback.clear()
    compositeDisposable.dispose()
  }

  fun process(text: String) {
    subject.onNext(text)
  }
}