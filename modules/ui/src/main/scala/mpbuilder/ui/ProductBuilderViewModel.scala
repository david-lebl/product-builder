package mpbuilder.ui

import mpbuilder.domain.model.*
import mpbuilder.domain.model.CheckoutStep.*
import mpbuilder.domain.service.*
import mpbuilder.domain.validation.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.weight.{WeightBreakdown, WeightCalculator}
import mpbuilder.domain.sample.*
import zio.prelude.Validation
import com.raquo.laminar.api.L.*
import org.scalajs.dom

/** How the customer will provide artwork for the configured product */
sealed trait ArtworkMode
object ArtworkMode:
  case class UploadArtwork(fileName: Option[String] = None) extends ArtworkMode
  case object DesignInEditor extends ArtworkMode

/** Per-component UI state */
case class ComponentState(
                           role: ComponentRole,
                           selectedMaterialId: Option[MaterialId] = None,
                           selectedInkConfig: Option[InkConfiguration] = None,
                           selectedFinishes: Map[FinishId, Option[FinishParameters]] = Map.empty,
                         )

/** Application state for the product builder */
case class BuilderState(
                         selectedCategoryId: Option[CategoryId] = None,
                         componentStates: Map[ComponentRole, ComponentState] = Map.empty,
                         linkedComponents: Boolean = true,
                         selectedPrintingMethodId: Option[PrintingMethodId] = None,
                         specifications: List[SpecValue] = List.empty,
                         validationErrors: List[String] = List.empty,
                         priceBreakdown: Option[PriceBreakdown] = None,
                         weightBreakdown: Option[WeightBreakdown] = None,
                         configuration: Option[ProductConfiguration] = None,
                         language: Language = Language.En,
                         basket: Basket = Basket(BasketId.unsafe("main-basket"), List.empty),
                         basketMessage: Option[String] = None,
                         artworkMode: ArtworkMode = ArtworkMode.UploadArtwork(None),
                         basketItemArtwork: Map[ConfigurationId, ArtworkMode] = Map.empty,
                         checkoutInfo: Option[CheckoutInfo] = None,
                       )

object ProductBuilderViewModel:

  val catalog: ProductCatalog = SampleCatalog.catalog
  val ruleset = SampleRules.ruleset
  val pricelist = SamplePricelist.pricelistCzkSheet

  val stateVar: Var[BuilderState] = Var(BuilderState())
  val state: Signal[BuilderState] = stateVar.signal

  // Event bus that fires when category changes, carries default specs for the category
  val specResetBus: EventBus[List[SpecValue]] = new EventBus[List[SpecValue]]

  // Get current language
  def currentLanguage: Signal[Language] = state.map(_.language)

  // Initialize language on app startup (does not persist - language is already from localStorage or browser detection)
  def initializeLanguage(lang: Language): Unit =
    stateVar.update(_.copy(language = lang))

  // Switch language and persist to localStorage
  def setLanguage(lang: Language): Unit =
    stateVar.update(_.copy(language = lang))
    // Persist the language preference using language code (e.g., "en", "cs")
    try
      dom.window.localStorage.setItem("selectedLanguage", lang.toCode)
    catch
      case _: scala.scalajs.js.JavaScriptException =>
  // If localStorage access fails, silently ignore (language still works for current session)

  // Get all categories as a list
  def allCategories: List[ProductCategory] = catalog.categories.values.toList

  // Update category selection — derives component states from category template
  def selectCategory(categoryId: CategoryId): Unit = {
    val categoryOpt = catalog.categories.get(categoryId)
    val componentStates = categoryOpt match
      case Some(cat) =>
        cat.components.map(ct => ct.role -> ComponentState(ct.role)).toMap
      case None =>
        Map.empty[ComponentRole, ComponentState]

    val defaultSpecs = categoryOpt match
      case Some(cat) => defaultSpecsForCategory(cat)
      case None      => List.empty
    
    val defaultPrintMethod = categoryOpt match
      case Some(value) => defaultPrintMethodForCategory(value)
      case None => None

    stateVar.update(state =>
      state.copy(
        selectedCategoryId = Some(categoryId),
        componentStates = componentStates,
        linkedComponents = true,
        selectedPrintingMethodId = defaultPrintMethod,
        specifications = defaultSpecs,
        validationErrors = List.empty,
        priceBreakdown = None,
        configuration = None,
      )
    )
    specResetBus.emit(defaultSpecs)
    autoRecalculate()
  }
  
  private def defaultPrintMethodForCategory(cat: ProductCategory): Option[PrintingMethodId] =
    CatalogQueryService.availablePrintingMethods(cat.id, catalog).headOption.map(_.id)

  private def defaultSpecsForCategory(cat: ProductCategory): List[SpecValue] =
    val kinds = cat.requiredSpecKinds
    val specs = List.newBuilder[SpecValue]
    if kinds.contains(SpecKind.Quantity) then
      specs += SpecValue.QuantitySpec(Quantity.unsafe(1))
    if kinds.contains(SpecKind.Size) then
      specs += SpecValue.SizeSpec(Dimension(210, 297)) // A4
    if kinds.contains(SpecKind.Orientation) then
      specs += SpecValue.OrientationSpec(Orientation.Portrait)
    if kinds.contains(SpecKind.FoldType) then
      specs += SpecValue.FoldTypeSpec(FoldType.Half)
    if kinds.contains(SpecKind.BindingMethod) then
      specs += SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch)
    if kinds.contains(SpecKind.Pages) then
      specs += SpecValue.PagesSpec(8)
    specs.result()

  // Update material selection for a specific component role
  def selectMaterial(role: ComponentRole, materialId: MaterialId): Unit = 
    stateVar.update(state =>
      if state.linkedComponents then
        val newStates = state.componentStates.map { case (r, c) =>
          r -> c.copy(selectedMaterialId = Some(materialId), selectedFinishes = Map.empty)
        }
        state.copy(componentStates = newStates)
      else
        val cs = state.componentStates.getOrElse(role, ComponentState(role))
        val updated = cs.copy(
          selectedMaterialId = Some(materialId),
          selectedFinishes = Map.empty, // Reset finishes when material changes
        )
        state.copy(componentStates = state.componentStates + (role -> updated))
    )
    autoRecalculate()

  // Toggle finish selection for a specific component role
  def toggleFinish(role: ComponentRole, finishId: FinishId, defaultParams: Option[FinishParameters] = None): Unit =
    stateVar.update(state =>
      val cs = state.componentStates.getOrElse(role, ComponentState(role))
      val newFinishes =
        if cs.selectedFinishes.contains(finishId) then
          cs.selectedFinishes - finishId
        else
          cs.selectedFinishes + (finishId -> defaultParams)
      // Finishes are always per-component — do not propagate even when components are linked
      state.copy(componentStates = state.componentStates + (role -> cs.copy(selectedFinishes = newFinishes)))
    )
    autoRecalculate()

  // Set finish parameters for a specific finish in a component
  def setFinishParams(role: ComponentRole, finishId: FinishId, params: Option[FinishParameters]): Unit =
    stateVar.update(state =>
      val cs = state.componentStates.getOrElse(role, ComponentState(role))
      if cs.selectedFinishes.contains(finishId) then
        val newFinishes = cs.selectedFinishes + (finishId -> params)
        // Finishes are always per-component — do not propagate even when components are linked
        state.copy(componentStates = state.componentStates + (role -> cs.copy(selectedFinishes = newFinishes)))
      else state
    )
    autoRecalculate()

  // Read current finish params (for use in event handlers)
  def currentFinishParams(role: ComponentRole, finishId: FinishId): Option[FinishParameters] =
    stateVar.now().componentStates.get(role).flatMap(_.selectedFinishes.get(finishId)).flatten

  // Select ink configuration for a specific component role
  def selectInkConfig(role: ComponentRole, config: InkConfiguration): Unit = 
    stateVar.update(state =>
      if state.linkedComponents then
        val newStates = state.componentStates.map { case (r, c) =>
          r -> c.copy(selectedInkConfig = Some(config))
        }
        state.copy(componentStates = newStates)
      else
        val cs = state.componentStates.getOrElse(role, ComponentState(role))
        state.copy(componentStates = state.componentStates + (role -> cs.copy(selectedInkConfig = Some(config))))
    )
    autoRecalculate()

  // Update printing method
  def selectPrintingMethod(methodId: PrintingMethodId): Unit = 
    stateVar.update(state =>
      state.copy(selectedPrintingMethodId = Some(methodId))
    )
    autoRecalculate()

  // Update specifications
  def updateSpecifications(specs: List[SpecValue]): Unit = 
    stateVar.update(state =>
      state.copy(specifications = specs)
    )
    autoRecalculate()

  // Add a specification
  def addSpecification(spec: SpecValue): Unit = 
    stateVar.update(state =>
      state.copy(specifications = state.specifications :+ spec)
    )
    autoRecalculate()

  // Remove a specification by type
  def removeSpecification(specType: Class[?]): Unit = 
    stateVar.update(state =>
      state.copy(specifications = state.specifications.filterNot(s => s.getClass == specType))
    )
    autoRecalculate()

  // Replace (or add) a specification atomically in a single state update
  def replaceSpecification(spec: SpecValue): Unit =
    stateVar.update(state =>
      val filtered = state.specifications.filterNot(s => s.getClass == spec.getClass)
      state.copy(specifications = filtered :+ spec)
    )
    autoRecalculate()

  // Automatically recalculate price when configuration inputs change
  private def autoRecalculate(): Unit =
    val currentState = stateVar.now()
    (currentState.selectedCategoryId,
      currentState.componentStates.values.flatMap(_.selectedMaterialId).headOption,
      currentState.selectedPrintingMethodId) match
      case (Some(_), Some(_), Some(_)) =>
        validateConfiguration()
      case _ =>
        // Not enough selections yet - clear price silently
        stateVar.update(_.copy(
          validationErrors = List.empty,
          priceBreakdown = None,
          weightBreakdown = None,
          configuration = None,
        ))

  // Build and validate configuration
  def validateConfiguration(): Unit = {
    val currentState = stateVar.now()
    val lang = currentState.language

    val allHaveMaterial = currentState.componentStates.nonEmpty &&
      currentState.componentStates.values.forall(_.selectedMaterialId.isDefined)
    val allHaveInkConfig = currentState.componentStates.nonEmpty &&
      currentState.componentStates.values.forall(_.selectedInkConfig.isDefined)

    (currentState.selectedCategoryId, currentState.selectedPrintingMethodId) match
      case (Some(categoryId), Some(printingMethodId)) if allHaveMaterial && allHaveInkConfig =>
        val components = currentState.componentStates.values.map { cs =>
          ComponentRequest(
            role = cs.role,
            materialId = cs.selectedMaterialId.get,
            inkConfiguration = cs.selectedInkConfig.get,
            finishes = cs.selectedFinishes.toList.map { case (id, params) => FinishSelection(id, params) },
          )
        }.toList

        val request = ConfigurationRequest(
          categoryId = categoryId,
          printingMethodId = printingMethodId,
          components = components,
          specs = currentState.specifications,
        )

        // Generate a unique configuration ID based on timestamp
        val configId = ConfigurationId.unsafe(s"config-${System.currentTimeMillis()}")

        // Validate
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)

        result.fold(
          errors => {
            // Validation failed
            val errorMessages = errors.map(_.message(lang)).toList
            stateVar.update(_.copy(
              configuration = None,
              validationErrors = errorMessages,
              priceBreakdown = None,
              weightBreakdown = None,
            ))
          },
          config => {
            // Validation succeeded, calculate price
            val priceResult = PriceCalculator.calculate(config, pricelist, lang)
            priceResult.fold(
              errors => {
                val errorMessages = errors.map(_.message(lang)).toList
                stateVar.update(_.copy(
                  configuration = Some(config),
                  validationErrors = errorMessages,
                  priceBreakdown = None,
                  weightBreakdown = None,
                ))
              },
              breakdown => {
                val weightBreakdownOpt = WeightCalculator.calculate(config).toOption
                stateVar.update(_.copy(
                  configuration = Some(config),
                  validationErrors = List.empty,
                  priceBreakdown = Some(breakdown),
                  weightBreakdown = weightBreakdownOpt,
                ))
              }
            )
          }
        )
      case _ =>
        val msg = lang match
          case Language.En => "Please select a category, printing method, and configure all components (material, ink configuration)"
          case Language.Cs => "Vyberte prosím kategorii, tiskovou metodu a nakonfigurujte všechny komponenty (materiál, konfigurace inkoustu)"
        stateVar.update(_.copy(
          validationErrors = List(msg),
          configuration = None,
          priceBreakdown = None,
          weightBreakdown = None,
        ))
  }

  // Get component roles for the currently selected category
  def componentRoles: Signal[List[ComponentRole]] =
    state.map(_.componentStates.keys.toList.sortBy(_.ordinal))

  // Whether all components share the same material/ink configuration
  def linkedComponents: Signal[Boolean] =
    state.map(_.linkedComponents)

  // Toggle whether all components share the same configuration
  def setLinkedComponents(linked: Boolean): Unit =
    stateVar.update { s =>
      if linked then
        // Sync material and ink config from the first (master) component to all others.
        // Finishes remain per-component — they are not shared even when linked.
        // getOrElse with a fallback is safe: roles are derived from componentStates.keys,
        // so roles.head is guaranteed to be present in the map.
        val roles = s.componentStates.keys.toList.sortBy(_.ordinal)
        if roles.size > 1 then
          val masterState = s.componentStates.getOrElse(roles.head, ComponentState(roles.head))
          val syncedStates = s.componentStates.map { case (role, cs) =>
            role -> cs.copy(
              selectedMaterialId = masterState.selectedMaterialId,
              selectedInkConfig = masterState.selectedInkConfig,
            )
          }
          s.copy(linkedComponents = true, componentStates = syncedStates)
        else
          s.copy(linkedComponents = true)
      else
        s.copy(linkedComponents = false)
    }
    autoRecalculate()

  // Get available materials for a specific component role
  def availableMaterials(role: ComponentRole): Signal[List[Material]] =
    state.map { s =>
      s.selectedCategoryId match
        case Some(categoryId) =>
          CatalogQueryService.availableMaterials(categoryId, catalog, role)
        case None =>
          List.empty
    }

  // Get available finishes for a specific component role
  def availableFinishes(role: ComponentRole): Signal[List[Finish]] =
    state.map { s =>
      val materialId = s.componentStates.get(role).flatMap(_.selectedMaterialId)
      (s.selectedCategoryId, materialId) match
        case (Some(categoryId), Some(matId)) =>
          CatalogQueryService.compatibleFinishes(
            categoryId,
            matId,
            catalog,
            ruleset,
            s.selectedPrintingMethodId,
            role,
          )
        case _ =>
          List.empty
    }

  // Get required spec kinds for the selected category
  def requiredSpecKinds: Signal[Set[SpecKind]] =
    state.map { s =>
      s.selectedCategoryId.flatMap(id => catalog.categories.get(id)) match
        case Some(cat) => cat.requiredSpecKinds
        case None => Set.empty
    }

  // Get available printing methods for selected category
  def availablePrintingMethods: Signal[List[PrintingMethod]] =
    state.map { s =>
      s.selectedCategoryId match
        case Some(categoryId) =>
          CatalogQueryService.availablePrintingMethods(categoryId, catalog)
        case None =>
          List.empty
    }

  // Per-component signals
  def selectedInkConfig(role: ComponentRole): Signal[Option[InkConfiguration]] =
    state.map(_.componentStates.get(role).flatMap(_.selectedInkConfig))

  def selectedMaterialId(role: ComponentRole): Signal[Option[MaterialId]] =
    state.map(_.componentStates.get(role).flatMap(_.selectedMaterialId))

  def selectedFinishIds(role: ComponentRole): Signal[Set[FinishId]] =
    state.map(_.componentStates.get(role).map(_.selectedFinishes.keySet).getOrElse(Set.empty))

  def selectedFinishParams(role: ComponentRole, finishId: FinishId): Signal[Option[FinishParameters]] =
    state.map(_.componentStates.get(role).flatMap(_.selectedFinishes.get(finishId)).flatten)

  def selectedOrientation: Signal[Option[Orientation]] =
    state.map { s =>
      s.specifications.collectFirst {
        case SpecValue.OrientationSpec(orientation) => orientation
      }
    }

  def selectedFoldType: Signal[Option[FoldType]] =
    state.map { s =>
      s.specifications.collectFirst {
        case SpecValue.FoldTypeSpec(foldType) => foldType
      }
    }

  def selectedBindingMethod: Signal[Option[BindingMethod]] =
    state.map { s =>
      s.specifications.collectFirst {
        case SpecValue.BindingMethodSpec(bindingMethod) => bindingMethod
      }
    }

  // Basket operations
  def addToBasket(quantity: Int): Unit =
    val currentState = stateVar.now()
    val lang = currentState.language

    currentState.configuration match
      case Some(config) =>
        val result = BasketService.addItem(currentState.basket, config, quantity, pricelist)
        result.fold(
          errors => {
            val errorMsg = errors.map(_.message(lang)).toList.mkString(", ")
            stateVar.update(_.copy(basketMessage = Some(errorMsg)))
          },
          updatedBasket => {
            val msg = lang match
              case Language.En => s"Added to basket (${quantity}×)"
              case Language.Cs => s"Přidáno do košíku (${quantity}×)"
            val updatedArtwork = currentState.basketItemArtwork + (config.id -> currentState.artworkMode)
            stateVar.update(_.copy(
              basket = updatedBasket,
              basketMessage = Some(msg),
              basketItemArtwork = updatedArtwork,
            ))
            resetProductForm()
          }
        )
      case None =>
        val msg = lang match
          case Language.En => "Please configure and validate a product first"
          case Language.Cs => "Nejprve nakonfigurujte a ověřte produkt"
        stateVar.update(_.copy(basketMessage = Some(msg)))

  def removeFromBasket(configId: ConfigurationId): Unit =
    val currentState = stateVar.now()
    val updatedBasket = BasketService.removeItem(currentState.basket, configId)
    val updatedArtwork = currentState.basketItemArtwork - configId
    stateVar.update(_.copy(basket = updatedBasket, basketMessage = None, basketItemArtwork = updatedArtwork))

  def updateBasketQuantity(configId: ConfigurationId, newQuantity: Int): Unit =
    val currentState = stateVar.now()
    val lang = currentState.language
    val result = BasketService.updateQuantity(currentState.basket, configId, newQuantity)
    result.fold(
      errors => {
        val errorMsg = errors.map(_.message(lang)).toList.mkString(", ")
        stateVar.update(_.copy(basketMessage = Some(errorMsg)))
      },
      updatedBasket => {
        stateVar.update(_.copy(basket = updatedBasket, basketMessage = None))
      }
    )

  def clearBasket(): Unit =
    val currentState = stateVar.now()
    val clearedBasket = BasketService.clear(currentState.basket)
    stateVar.update(_.copy(basket = clearedBasket, basketMessage = None, basketItemArtwork = Map.empty))

  def basketCalculation: Signal[BasketCalculation] =
    state.map(s => BasketService.calculateTotal(s.basket))

  def clearBasketMessage(): Unit =
    stateVar.update(_.copy(basketMessage = None))

  /** Reset all product-form fields back to their initial state while preserving basket and language. */
  def resetProductForm(): Unit =
    stateVar.update(_.copy(
      selectedCategoryId = None,
      componentStates = Map.empty,
      linkedComponents = true,
      selectedPrintingMethodId = None,
      specifications = List.empty,
      validationErrors = List.empty,
      priceBreakdown = None,
      weightBreakdown = None,
      configuration = None,
      artworkMode = ArtworkMode.UploadArtwork(None),
    ))

  // Artwork operations
  def setArtworkMode(mode: ArtworkMode): Unit =
    stateVar.update(_.copy(artworkMode = mode))

  def setUploadedFileName(name: Option[String]): Unit =
    stateVar.update(_.copy(artworkMode = ArtworkMode.UploadArtwork(name)))

  // Checkout operations
  def startCheckout(): Unit =
    stateVar.update(_.copy(checkoutInfo = Some(CheckoutInfo())))

  def cancelCheckout(): Unit =
    stateVar.update(_.copy(checkoutInfo = None))

  def updateCheckoutInfo(info: CheckoutInfo): Unit =
    stateVar.update(_.copy(checkoutInfo = Some(info)))

  def checkoutNextStep(): Unit =
    stateVar.update { s =>
      s.checkoutInfo match
        case Some(info) =>
          val nextStep = info.step match
            case CheckoutStep.Authentication  => CheckoutStep.ContactDetails
            case CheckoutStep.ContactDetails  => CheckoutStep.Delivery
            case CheckoutStep.Delivery        => CheckoutStep.Payment
            case CheckoutStep.Payment         => CheckoutStep.Summary
            case CheckoutStep.Summary         => CheckoutStep.Summary // terminal — order confirmed from this step
          s.copy(checkoutInfo = Some(info.copy(step = nextStep)))
        case None => s
    }

  def checkoutPrevStep(): Unit =
    stateVar.update { s =>
      s.checkoutInfo match
        case Some(info) =>
          val prevStep = info.step match
            case CheckoutStep.Authentication  => CheckoutStep.Authentication
            case CheckoutStep.ContactDetails  => CheckoutStep.Authentication
            case CheckoutStep.Delivery        => CheckoutStep.ContactDetails
            case CheckoutStep.Payment         => CheckoutStep.Delivery
            case CheckoutStep.Summary         => CheckoutStep.Payment
          s.copy(checkoutInfo = Some(info.copy(step = prevStep)))
        case None => s
    }

  /** Available shop pickup locations */
  val shopLocations: List[ShopLocation] = List(
    ShopLocation("shop-prague",    LocalizedString("Prague — Wenceslas Square 1",    "Praha — Václavské náměstí 1"),
                                   LocalizedString("Wenceslas Square 1, 110 00 Prague 1", "Václavské náměstí 1, 110 00 Praha 1")),
    ShopLocation("shop-brno",      LocalizedString("Brno — Freedom Square 5",        "Brno — náměstí Svobody 5"),
                                   LocalizedString("Freedom Square 5, 602 00 Brno",  "náměstí Svobody 5, 602 00 Brno")),
    ShopLocation("shop-ostrava",   LocalizedString("Ostrava — Masaryk Square 3",     "Ostrava — Masarykovo náměstí 3"),
                                   LocalizedString("Masaryk Square 3, 702 00 Ostrava", "Masarykovo náměstí 3, 702 00 Ostrava")),
  )

  /** Available courier services */
  val courierServices: List[CourierService] = List(
    CourierService("courier-standard", LocalizedString("Standard Delivery",   "Standardní doručení"),
                                       LocalizedString("3–5 business days",   "3–5 pracovních dní"),
                                       Money("99.00"), Currency.CZK),
    CourierService("courier-express",  LocalizedString("Express Delivery",    "Expresní doručení"),
                                       LocalizedString("1–2 business days",   "1–2 pracovní dny"),
                                       Money("249.00"), Currency.CZK),
    CourierService("courier-economy",  LocalizedString("Economy Delivery",    "Ekonomické doručení"),
                                       LocalizedString("5–10 business days",  "5–10 pracovních dní"),
                                       Money("49.00"), Currency.CZK),
  )
