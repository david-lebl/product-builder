package mpbuilder.orderintake
package impl
package memory

import mpbuilder.commons.*
import zio.*

/** The shop's delivery options, hardcoded.
  *
  * These three couriers and three pickup points lived in the SPA's view model, which meant the
  * prices a customer was shown existed only in the browser. They are the same values, moved to the
  * only side that can be trusted with them. A real table replaces this class and nothing else.
  *
  * Surcharges are quoted in CZK; a request in another currency gets pickup only rather than a
  * silently mis-converted price.
  */
private[orderintake] object StaticDeliveryCatalog extends DeliveryCatalog:

  private val pickupPoints = List(
    DeliveryOption(
      id = "shop-prague",
      kind = DeliveryKind.Pickup,
      name = LocalizedString("Prague — Wenceslas Square 1", "Praha — Václavské náměstí 1"),
      detail = LocalizedString(
        "Wenceslas Square 1, 110 00 Prague 1",
        "Václavské náměstí 1, 110 00 Praha 1",
      ),
      surcharge = Money.zero,
      currency = Currency.CZK,
    ),
    DeliveryOption(
      id = "shop-brno",
      kind = DeliveryKind.Pickup,
      name = LocalizedString("Brno — Freedom Square 5", "Brno — náměstí Svobody 5"),
      detail = LocalizedString("Freedom Square 5, 602 00 Brno", "náměstí Svobody 5, 602 00 Brno"),
      surcharge = Money.zero,
      currency = Currency.CZK,
    ),
    DeliveryOption(
      id = "shop-ostrava",
      kind = DeliveryKind.Pickup,
      name = LocalizedString("Ostrava — Masaryk Square 3", "Ostrava — Masarykovo náměstí 3"),
      detail = LocalizedString(
        "Masaryk Square 3, 702 00 Ostrava",
        "Masarykovo náměstí 3, 702 00 Ostrava",
      ),
      surcharge = Money.zero,
      currency = Currency.CZK,
    ),
  )

  private val couriers = List(
    DeliveryOption(
      id = "courier-standard",
      kind = DeliveryKind.Courier,
      name = LocalizedString("Standard delivery", "Standardní doručení"),
      detail = LocalizedString("3–5 business days", "3–5 pracovních dní"),
      surcharge = Money("99.00"),
      currency = Currency.CZK,
    ),
    DeliveryOption(
      id = "courier-express",
      kind = DeliveryKind.Courier,
      name = LocalizedString("Express delivery", "Expresní doručení"),
      detail = LocalizedString("1–2 business days", "1–2 pracovní dny"),
      surcharge = Money("249.00"),
      currency = Currency.CZK,
    ),
    DeliveryOption(
      id = "courier-economy",
      kind = DeliveryKind.Courier,
      name = LocalizedString("Economy delivery", "Ekonomické doručení"),
      detail = LocalizedString("5–10 business days", "5–10 pracovních dní"),
      surcharge = Money("49.00"),
      currency = Currency.CZK,
    ),
  )

  def options(currency: Currency): UIO[List[DeliveryOption]] =
    ZIO.succeed(
      if currency == Currency.CZK then pickupPoints ++ couriers
      // Collecting the goods costs nothing in any currency, so pickup survives the filter honestly.
      else pickupPoints.map(_.copy(currency = currency))
    )
