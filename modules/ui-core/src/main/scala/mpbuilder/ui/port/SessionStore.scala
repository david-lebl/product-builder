package mpbuilder.ui.port

import kyo.*
import mpbuilder.identity.SessionToken
import mpbuilder.shared.CartId

/** What the storefront remembers across page reloads and browser tabs: the signed-in session token and the
  * active cart. Both are only handles — the backend stays the source of truth and is asked to re-validate
  * them on boot (`StorefrontClient.restoreSession` / `getCart`).
  */
final case class PersistedSession(sessionToken: Option[SessionToken], cartId: Option[CartId])

/** Platform port for persisting [[PersistedSession]]; the browser SPA backs it with `localStorage`. */
trait SessionStore:
  def load: PersistedSession < Sync
  def saveSessionToken(token: Option[SessionToken]): Unit < Sync
  def saveCartId(cartId: Option[CartId]): Unit < Sync

object SessionStore:

  /** Remembers nothing (tests, embeds without persistent storage). */
  val none: SessionStore = new SessionStore:
    def load: PersistedSession < Sync                              = PersistedSession(None, None)
    def saveSessionToken(token: Option[SessionToken]): Unit < Sync = Kyo.unit
    def saveCartId(cartId: Option[CartId]): Unit < Sync            = Kyo.unit

  /** In-memory store, e.g. to simulate a page reload in JVM tests by booting a second state over it. */
  def inMemory(initial: PersistedSession = PersistedSession(None, None)): SessionStore < Sync =
    AtomicRef.init(initial).map { ref =>
      new SessionStore:
        def load: PersistedSession < Sync                              = ref.get
        def saveSessionToken(token: Option[SessionToken]): Unit < Sync = ref.updateAndGet(_.copy(sessionToken = token)).unit
        def saveCartId(cartId: Option[CartId]): Unit < Sync            = ref.updateAndGet(_.copy(cartId = cartId)).unit
    }
