package mpbuilder.manufacturing
package impl
package http

import mpbuilder.commons.Language

/** Reading `Accept-Language`. The same two-language rule order-intake applies, kept here rather
  * than shared because a context depending on another context's HTTP plumbing would be a
  * dependency in exactly the direction the boundaries exist to prevent.
  */
private[manufacturing] object Languages:

  def parse(header: Option[String]): Language =
    header
      .map(_.split(',').toList)
      .getOrElse(Nil)
      .flatMap { entry =>
        val tag = entry.takeWhile(_ != ';').trim.toLowerCase
        Language.values.find(l => tag == l.toCode || tag.startsWith(s"${l.toCode}-"))
      }
      .headOption
      .getOrElse(Language.En)
