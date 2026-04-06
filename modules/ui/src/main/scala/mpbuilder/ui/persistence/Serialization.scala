package mpbuilder.ui.persistence

import scala.scalajs.js
import zio.json.*
import EditorCodecs.given
import mpbuilder.ui.visualeditor.*
import org.scalajs.dom

/** Bridges between Scala model types and native JavaScript objects for IndexedDB storage.
  *
  * Why `js.Dynamic`?  IndexedDB stores and retrieves native JavaScript objects (via the
  * Structured Clone Algorithm), not JSON strings.  The put/get API therefore requires
  * `js.Dynamic`, not a Scala class.
  *
  * Why not write manual field-by-field conversions?  With zio-json we derive the codecs
  * automatically from the model structure.  The bridge is then simply:
  *   Scala → `toJson` (JSON String) → `js.JSON.parse` → js.Dynamic  → IndexedDB
  *   IndexedDB → js.Dynamic → `js.JSON.stringify` (JSON String) → `fromJson[A]` → Scala
  *
  * Adding or renaming a field on any model class is automatically reflected here without
  * touching this file.
  *
  * Note: if you change the model structure after data has been stored in IndexedDB you
  * should bump `EditorSessionStore.DB_VERSION` so the browser opens a fresh database and
  * the old incompatible records are discarded.
  */
object Serialization:

  def sessionToJs(s: EditorSession): js.Dynamic =
    js.JSON.parse(s.toJson)

  def sessionFromJs(d: js.Dynamic): EditorSession =
    decode[EditorSession](d, "session")

  def imageReferenceToJs(img: ImageReference): js.Dynamic =
    js.JSON.parse(img.toJson)

  def imageReferenceFromJs(d: js.Dynamic): ImageReference =
    decode[ImageReference](d, "image")

  private def decode[A: JsonDecoder](d: js.Dynamic, label: String): A =
    js.JSON.stringify(d).fromJson[A] match
      case Right(value) => value
      case Left(err) =>
        dom.console.error(s"Failed to deserialize $label: $err")
        throw new RuntimeException(s"Failed to deserialize $label: $err")
