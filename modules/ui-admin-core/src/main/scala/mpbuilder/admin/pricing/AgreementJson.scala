package mpbuilder.admin.pricing

import kyo.*
import mpbuilder.json.JsonSupport
import mpbuilder.json.PricingCodecs.given
import mpbuilder.pricing.CustomerPriceAgreement

/** JSON mode of the agreement editor: the exact wire / JSONB shape of `CustomerPriceAgreement`.
  *
  * Decoding goes through the shared kyo-schema codecs, so opaque values (percentages, IDs, non-empty
  * tier lists) are validated by the same smart constructors as the HTTP API and the database.
  */
object AgreementJson:

  def encode(a: CustomerPriceAgreement): String = pretty(JsonSupport.toJson(a))

  def decode(json: String): Either[String, CustomerPriceAgreement] =
    if json.trim.isEmpty then Left("JSON is empty")
    else
      scala.util.Try(JsonSupport.fromJson[CustomerPriceAgreement](json)).toEither.left
        .map(e => String.valueOf(e.getMessage))
        .flatten

  /** Re-indents compact JSON (2 spaces); string contents are left untouched. */
  def pretty(compact: String): String =
    val sb       = new StringBuilder
    var indent   = 0
    var inString = false
    var escaped  = false
    def newline(): Unit = { sb.append('\n'); sb.append("  " * indent) }
    var i = 0
    while i < compact.length do
      val c = compact.charAt(i)
      if inString then
        sb.append(c)
        if escaped then escaped = false
        else if c == '\\' then escaped = true
        else if c == '"' then inString = false
      else
        c match
          case '"' => inString = true; sb.append(c)
          case '{' | '[' =>
            val close = if c == '{' then '}' else ']'
            val next  = compact.drop(i + 1).dropWhile(_.isWhitespace).headOption
            if next.contains(close) then
              sb.append(c).append(close)
              i = compact.indexOf(close, i + 1)
            else
              sb.append(c); indent += 1; newline()
          case '}' | ']' => indent -= 1; newline(); sb.append(c)
          case ','       => sb.append(c); newline()
          case ':'       => sb.append(": ")
          case w if w.isWhitespace => ()
          case other     => sb.append(other)
      i += 1
    sb.result()
