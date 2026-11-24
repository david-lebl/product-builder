package mpbuilder.storefront

import kyo.*
import mpbuilder.pricing.*
import mpbuilder.shared.*
import java.math.MathContext

/** Outbound port providing currency exchange rates to the Storefront BFF.
  *
  * Ships with `ExchangeRateProvider.hardcoded` (fixed CZK ↔ EUR rate) and can be
  * backed by an external adapter (e.g. ČNB, ECB, or database-backed rates) in the future.
  */
trait ExchangeRateProvider:
  /** Returns the multiplier converting 1 unit of `from` into `to`. */
  def rate(from: Currency, to: Currency): BigDecimal < (Async & Abort[StorefrontError])

object ExchangeRateProvider:
  /** Default hardcoded exchange rate: 1 EUR = 25.00 CZK. */
  val defaultCzkPerEur: BigDecimal = BigDecimal("25.00")

  /** Simple hardcoded adapter for CZK ↔ EUR conversion. */
  def hardcoded(czkPerEur: BigDecimal = defaultCzkPerEur): ExchangeRateProvider =
    new ExchangeRateProvider:
      private val eurPerCzk: BigDecimal =
        BigDecimal(BigDecimal(1).bigDecimal.divide(czkPerEur.bigDecimal, MathContext.DECIMAL128))

      override def rate(from: Currency, to: Currency): BigDecimal < (Async & Abort[StorefrontError]) =
        (from, to) match
          case (a, b) if a == b             => BigDecimal(1)
          case (Currency.CZK, Currency.EUR) => eurPerCzk
          case (Currency.EUR, Currency.CZK) => czkPerEur
          case _                            => Abort.fail(StorefrontError.UnsupportedCurrencyConversion(from, to))

/** Storefront-layer currency conversion service backed by an `ExchangeRateProvider` adapter. */
final class CurrencyConversionService(provider: ExchangeRateProvider):

  def getExchangeRate(from: Currency, to: Currency): ExchangeRateQuote < (Async & Abort[StorefrontError]) =
    provider.rate(from, to).map(r => ExchangeRateQuote(from, to, r))

  def convertMoney(amount: Money, from: Currency, to: Currency): Price < (Async & Abort[StorefrontError]) =
    if from == to then Price(amount.rounded, to)
    else provider.rate(from, to).map(r => Price((amount * r).rounded, to))

  def convertQuoteResult(
      result: ProductBuilderQuoteResult,
      targetCurrency: Currency,
  ): ProductBuilderQuoteResult < (Async & Abort[StorefrontError]) =
    val sourceCurrency = result.pricing.effectiveQuote.currency
    if sourceCurrency == targetCurrency then result
    else
      provider.rate(sourceCurrency, targetCurrency).map { rate =>
        def conv(m: Money): Money = (m * rate).rounded

        def convChargeLine(l: ChargeLine): ChargeLine =
          l.copy(unitPrice = conv(l.unitPrice), total = conv(l.total))

        def convQuote(q: PriceQuote): PriceQuote =
          val net   = conv(q.netTotal)
          val taxA  = q.tax.rate.taxOnNet(net)
          val gross = (net + taxA).rounded
          q.copy(
            currency = targetCurrency,
            componentQuotes = q.componentQuotes.map { cq =>
              cq.copy(
                materialLine = convChargeLine(cq.materialLine),
                cuttingLine = cq.cuttingLine.map(convChargeLine),
                inkLine = cq.inkLine.map(convChargeLine),
                finishLines = cq.finishLines.map(convChargeLine),
              )
            },
            orderCharges = q.orderCharges.map(convChargeLine),
            variableSubtotalNet = conv(q.variableSubtotalNet),
            discountedSubtotalNet = conv(q.discountedSubtotalNet),
            speedAdjustment = q.speedAdjustment.map(convChargeLine),
            setupFees = q.setupFees.map(convChargeLine),
            minimumAdjustment = q.minimumAdjustment.map(convChargeLine),
            promoDiscount = q.promoDiscount.map(convChargeLine),
            tax = TaxSummary(q.tax.rate, net, taxA, gross),
            netTotal = net,
            grossTotal = gross,
          )

        val effConv    = convQuote(result.pricing.effectiveQuote)
        val walkInConv = result.pricing.walkInBaseQuote.map(convQuote)
        val savingsNet = walkInConv
          .map(w => (w.netTotal - effConv.netTotal).rounded)
          .orElse(result.pricing.savingsNet.map(conv))

        result.copy(
          pricing = result.pricing.copy(
            effectiveQuote = effConv,
            walkInBaseQuote = walkInConv,
            savingsNet = savingsNet,
          ),
          localizedBreakdown = result.localizedBreakdown.map { l =>
            l.copy(unitPrice = conv(l.unitPrice), total = conv(l.total))
          },
        )
      }

object CurrencyConversionService:
  def make(provider: ExchangeRateProvider = ExchangeRateProvider.hardcoded()): CurrencyConversionService =
    new CurrencyConversionService(provider)
