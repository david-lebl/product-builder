package mpbuilder.ui.basket

import org.scalajs.dom

import scala.scalajs.js
import scala.util.Try

/** The token that tells the server which anonymous basket is this visitor's.
  *
  * Kept in `localStorage`, which is exactly what makes the basket survive a reload: the items live
  * on the server, and this is the only thing the browser has to remember to find them again.
  *
  * It is not a credential. It identifies a basket, nothing more — anyone who signs in gets a real
  * basket keyed to their customer id, and the anonymous one is folded into it and destroyed.
  */
private[ui] object BasketSession:

  private val storageKey = "mpbuilder.basket.session"

  /** This visitor's token, minted on first use and remembered from then on.
    *
    * Falls back to a per-page token when storage is unavailable — private browsing, or a browser
    * with storage disabled. The basket then behaves as it did before this switch: it works for as
    * long as the tab is open. That is a worse experience, not a broken one.
    */
  lazy val token: String =
    stored.getOrElse {
      val fresh = mint()
      Try(dom.window.localStorage.setItem(storageKey, fresh))
      fresh
    }

  private def stored: Option[String] =
    Try(Option(dom.window.localStorage.getItem(storageKey))).toOption.flatten
      .map(_.trim)
      .filter(_.nonEmpty)

  private def mint(): String =
    s"session-${js.Date.now().toLong}-${(js.Math.random() * 1e9).toLong}"
