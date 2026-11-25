package mpbuilder.sample.browser

import kyo.*
import mpbuilder.sample.{SampleShowcaseRuntime, SampleStorage, ShowcaseRuntime}
import mpbuilder.shared.*

/** The sample runtime of the static demo SPAs, persisted in this browser's IndexedDB when available. */
final case class BrowserShowcaseRuntime(runtime: ShowcaseRuntime, store: Option[DocStore]):

  def isPersistent: Boolean = store.isDefined

  /** Forgets every stored change; the caller reloads the page to start again from the sample data. */
  def resetStoredData(using Frame): Unit < Async =
    store.fold(Kyo.unit)(SampleStorage.reset)

object BrowserShowcaseRuntime:

  def make()(using Frame): BrowserShowcaseRuntime < Async =
    for
      store   <- IndexedDbDocStore.open()
      loaded  <- load(store)
      runtime <- SampleShowcaseRuntime.make(storage = loaded._1)
    yield BrowserShowcaseRuntime(runtime, loaded._2)

  private def load(store: Option[DocStore])(using Frame): (SampleStorage, Option[DocStore]) < Async =
    store match
      case Some(s) =>
        Abort.run[Throwable](Abort.catching[Throwable](SampleStorage.load(s))).map {
          case Result.Success(storage) => (storage, store)
          case failure =>
            Log.warn(s"Stored demo data could not be loaded, running in memory: $failure").andThen((SampleStorage.memory, None))
        }
      case None => (SampleStorage.memory, None)
