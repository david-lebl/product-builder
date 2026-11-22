package mpbuilder.json

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import mpbuilder.json.JsonSupport.*
import mpbuilder.json.SharedCodecs.given
import mpbuilder.manufacturing.*

/** Codecs for the parts of the `manufacturing` model surfaced through the BFF facades. */
object ManufacturingCodecs:
  given JsonValueCodec[ManufacturingError] = JsonCodecMaker.make(config)
