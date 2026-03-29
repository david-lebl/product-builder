package mpbuilder.ui.orders

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.service.*
import mpbuilder.domain.sample.*
import mpbuilder.domain.rules.*

/** Reactive state management for the internal order entry UI.
  *
  * Manages customer selection, order line items with pricing, and custom price overrides.
  */
object InternalOrderEntryViewModel:

  private val catalog: ProductCatalog = SampleCatalog.catalog
  private val ruleset: CompatibilityRuleset = SampleRules.ruleset
  private val pricelist: Pricelist = SamplePricelist.pricelistCzkSheet

  private val stateVar: Var[InternalOrderEntryState] = Var(InternalOrderEntryState())
  val state: Signal[InternalOrderEntryState] = stateVar.signal

  // ── Derived signals ────────────────────────────────────────────────────

  val selectedCustomer: Signal[Option[Customer]] = state.map(_.selectedCustomer)
  val lineItems: Signal[List[OrderLineItem]] = state.map(_.lineItems)
  val activeSection: Signal[OrderEntrySection] = state.map(_.activeSection)

  val orderTotal: Signal[Money] = lineItems.map { items =>
    items.foldLeft(Money.zero) { (acc, item) =>
      Money(acc.value + item.effectivePrice.value)
    }
  }

  val orderCost: Signal[Money] = lineItems.map { items =>
    items.foldLeft(Money.zero) { (acc, item) =>
      Money(acc.value + item.estimatedCost.value)
    }
  }

  val orderMargin: Signal[Money] = orderTotal.combineWith(orderCost).map { case (total, cost) =>
    Money(total.value - cost.value).rounded
  }

  val orderMarginPercent: Signal[BigDecimal] = orderTotal.combineWith(orderCost).map { case (total, cost) =>
    if total.value > 0 then
      ((total.value - cost.value) / total.value * 100).setScale(1, BigDecimal.RoundingMode.HALF_UP)
    else BigDecimal(0)
  }

  // ── Customer selection ─────────────────────────────────────────────────

  val allCustomers: Signal[List[Customer]] = Val(SampleCustomers.all)

  def selectCustomer(customer: Customer): Unit =
    stateVar.update(_.copy(selectedCustomer = Some(customer)))

  def clearCustomer(): Unit =
    stateVar.update(_.copy(selectedCustomer = None))

  // ── Section navigation ─────────────────────────────────────────────────

  def setSection(section: OrderEntrySection): Unit =
    stateVar.update(_.copy(activeSection = section))

  // ── Line item management ───────────────────────────────────────────────

  def addLineItem(): Unit =
    stateVar.update { s =>
      val newItem = OrderLineItem(lineId = s"line-${s.nextLineId}")
      s.copy(lineItems = s.lineItems :+ newItem, nextLineId = s.nextLineId + 1)
    }

  def removeLineItem(lineId: String): Unit =
    stateVar.update(s => s.copy(lineItems = s.lineItems.filterNot(_.lineId == lineId)))

  def duplicateLineItem(lineId: String): Unit =
    stateVar.update { s =>
      s.lineItems.find(_.lineId == lineId) match
        case Some(item) =>
          val newItem = item.copy(lineId = s"line-${s.nextLineId}", customPriceOverride = None)
          s.copy(lineItems = s.lineItems :+ newItem, nextLineId = s.nextLineId + 1)
        case None => s
    }

  def updateLineItem(lineId: String, f: OrderLineItem => OrderLineItem): Unit =
    stateVar.update { s =>
      val updated = s.lineItems.map { item =>
        if item.lineId == lineId then
          val modified = f(item)
          recalculate(modified)
        else item
      }
      s.copy(lineItems = updated)
    }

  def setCustomPrice(lineId: String, price: Option[BigDecimal]): Unit =
    stateVar.update { s =>
      s.copy(lineItems = s.lineItems.map { item =>
        if item.lineId == lineId then item.copy(customPriceOverride = price)
        else item
      })
    }

  def setOrderNotes(notes: String): Unit =
    stateVar.update(_.copy(orderNotes = notes))

  // ── Price recalculation ────────────────────────────────────────────────

  private def recalculate(item: OrderLineItem): OrderLineItem =
    val configOpt = buildConfiguration(item)
    configOpt match
      case Some(config) =>
        val priceResult = PriceCalculator.calculate(config, pricelist)
        priceResult.toEither match
          case Right(breakdown) =>
            item.copy(configuration = Some(config), priceBreakdown = Some(breakdown))
          case Left(_) =>
            item.copy(configuration = Some(config), priceBreakdown = None)
      case None =>
        item.copy(configuration = None, priceBreakdown = None)

  private def buildConfiguration(item: OrderLineItem): Option[ProductConfiguration] =
    for
      catId <- item.categoryId
      pmId  <- item.printingMethodId
      matId <- item.materialId
      if item.width > 0 && item.height > 0 && item.quantity > 0
      config <- {
        val finishSelections = item.finishIds.map(id => FinishSelection(id))
        val components = List(ComponentRequest(ComponentRole.Main, matId, item.inkConfiguration, finishSelections))
        val specs = List(
          SpecValue.SizeSpec(Dimension(item.width, item.height)),
          SpecValue.QuantitySpec(Quantity.unsafe(item.quantity)),
        ) ++ item.foldType.map(ft => SpecValue.FoldTypeSpec(ft)).toList
          ++ item.bindingMethod.map(bm => SpecValue.BindingMethodSpec(bm)).toList
          ++ (if item.pages > 0 then List(SpecValue.PagesSpec(item.pages)) else Nil)

        val configId = ConfigurationId.unsafe(s"internal-${item.lineId}")
        ConfigurationBuilder.build(
          ConfigurationRequest(catId, pmId, components, specs),
          catalog,
          ruleset,
          configId,
        ).toEither.toOption
      }
    yield config

  // ── Reset ──────────────────────────────────────────────────────────────

  def resetOrder(): Unit =
    stateVar.set(InternalOrderEntryState())

  // ── Catalog access ─────────────────────────────────────────────────────

  def getCatalog: ProductCatalog = catalog
  def getPricelist: Pricelist = pricelist

  def categoriesSorted: List[(CategoryId, ProductCategory)] =
    catalog.categories.toList.sortBy(_._2.name.value)

  def materialsForCategory(categoryId: CategoryId): List[(MaterialId, Material)] =
    catalog.categories.get(categoryId) match
      case Some(cat) =>
        cat.components.headOption match
          case Some(template) =>
            template.allowedMaterialIds.toList.flatMap { matId =>
              catalog.materials.get(matId).map(mat => (matId, mat))
            }.sortBy(_._2.name.value)
          case None => Nil
      case None => Nil

  def printingMethodsForCategory(categoryId: CategoryId): List[(PrintingMethodId, PrintingMethod)] =
    catalog.categories.get(categoryId) match
      case Some(cat) =>
        val pmIds = if cat.allowedPrintingMethodIds.isEmpty then catalog.printingMethods.keySet
                    else cat.allowedPrintingMethodIds
        pmIds.toList.flatMap { pmId =>
          catalog.printingMethods.get(pmId).map(pm => (pmId, pm))
        }.sortBy(_._2.name.value)
      case None => Nil

  def finishesForCategory(categoryId: CategoryId): List[(FinishId, Finish)] =
    catalog.categories.get(categoryId) match
      case Some(cat) =>
        cat.components.headOption match
          case Some(template) =>
            template.allowedFinishIds.toList.flatMap { finId =>
              catalog.finishes.get(finId).map(fin => (finId, fin))
            }.sortBy(_._2.name.value)
          case None => Nil
      case None => Nil
