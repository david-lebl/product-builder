package mpbuilder.api

import java.util.UUID

import mpbuilder.domain.*
import mpbuilder.domain.catalog.Catalog
import mpbuilder.domain.config.*
import mpbuilder.domain.pricing.{PriceBreakdown, Pricelist}
import mpbuilder.domain.rules.CompatibilityRule
import zio.json.*

enum OrderStatus derives JsonCodec:
  case Placed

/** Everything the client needs to run the configurator locally: the catalog,
  * the presets, the compatibility rules, and the active pricelist — one source
  * of truth shared with the server.
  */
final case class CatalogResponse(
  catalog: Catalog,
  presets: List[Preset],
  rules: List[CompatibilityRule],
  pricelist: Pricelist,
) derives JsonCodec

final case class CreateOrderRequest(
  contact: CustomerContact,
  configuration: ProductConfiguration,
) derives JsonCodec

final case class OrderResponse(
  id: UUID,
  createdAt: java.time.Instant,
  status: OrderStatus,
  contact: CustomerContact,
  configuration: ProductConfiguration,
  breakdown: PriceBreakdown,
) derives JsonCodec

final case class OrderSummary(
  id: UUID,
  createdAt: java.time.Instant,
  customerName: String,
  categoryId: String,
  totalAmount: BigDecimal,
  currency: Currency,
  status: OrderStatus,
) derives JsonCodec

final case class ErrorDto(code: String, message: LocalizedText) derives JsonCodec

/** 422 payload: every accumulated validation/pricing error at once. */
final case class ApiError(errors: List[ErrorDto]) derives JsonCodec
