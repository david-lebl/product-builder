package mpbuilder.ui.basket

import mpbuilder.catalog.codec.ConfigurationCodecs.given
import mpbuilder.catalog.model.ProductConfiguration
import mpbuilder.catalog.model.*
import zio.json.*

/** Reading a stored configuration back out of a basket line.
  *
  * The server keeps the configuration catalog resolved and hands it back verbatim, which is what
  * lets a reloaded page render a basket it never built.
  *
  * Alone in its own file because the legacy domain's codecs and the kernel's both define an
  * encoding for `Money`; importing them into one file is ambiguous, and the honest fix is to let
  * each set of codecs have the file it is needed in.
  */
private[ui] object ConfigurationPayloads:
  def decode(payload: String): Either[String, ProductConfiguration] =
    payload.fromJson[ProductConfiguration]
