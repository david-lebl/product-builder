package mpbuilder.catalog

import mpbuilder.commons.json.given
import zio.json.*

/** JSON codecs for catalog's public wire shapes.
  *
  * They live with the DTOs, not with a consumer, so every caller encodes a configuration request
  * the same way — including the front end, which builds one in the browser.
  */
object json:

  given JsonCodec[FinishParamsDto] = DeriveJsonCodec.gen[FinishParamsDto]
  given JsonCodec[FinishSelectionDto] = DeriveJsonCodec.gen[FinishSelectionDto]
  given JsonCodec[InkSetupDto] = DeriveJsonCodec.gen[InkSetupDto]
  given JsonCodec[InkConfigurationDto] = DeriveJsonCodec.gen[InkConfigurationDto]
  given JsonCodec[ComponentRequestDto] = DeriveJsonCodec.gen[ComponentRequestDto]
  given JsonCodec[SizeDto] = DeriveJsonCodec.gen[SizeDto]
  given JsonCodec[SpecificationsDto] = DeriveJsonCodec.gen[SpecificationsDto]
  given JsonCodec[ConfigurationRequestDto] = DeriveJsonCodec.gen[ConfigurationRequestDto]

  given JsonCodec[CatalogVersion] = DeriveJsonCodec.gen[CatalogVersion]
  given JsonCodec[ConfigurationSnapshot] = DeriveJsonCodec.gen[ConfigurationSnapshot]
  given JsonCodec[ConfigurationView] = DeriveJsonCodec.gen[ConfigurationView]
