package mpbuilder.orderintake
package impl
package http

import mpbuilder.commons.Language

/** Reading `Accept-Language`.
  *
  * The domain has carried bilingual messages all along; without this the API threw the Czech half
  * away at the last step and told every customer what was wrong in English.
  *
  * Deliberately simple: the first tag that names a language we speak wins. Full RFC 9110 quality
  * ordering buys nothing here, because there are exactly two languages and no regional variants.
  */
private[orderintake] object Languages:

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
