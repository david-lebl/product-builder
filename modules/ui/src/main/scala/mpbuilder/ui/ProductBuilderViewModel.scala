package mpbuilder.ui

import mpbuilder.domain.model.*
import mpbuilder.domain.service.*
import mpbuilder.domain.validation.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.sample.*
import zio.prelude.Validation
import com.raquo.laminar.api.L.*
import org.scalajs.dom

/** Per-component UI state */
case class ComponentState(
                           role: ComponentRole,
                           selectedMaterialId: Option[MaterialId] = None,
                           selectedInkConfig: Option[InkConfiguration] = None,
                           selectedFinishIds: Set[FinishId] = Set.empty,
                         )

/** Application state for the product builder */
case class BuilderState(
                         selectedCategoryId: Option[CategoryId] = None,
                         componentStates: Map[ComponentRole, ComponentState] = Map.empty,
                         selectedPrintingMethodId: Option[PrintingMethodId] = None,
                         specifications: List[SpecValue] = List.empty,
                         validationErrors: List[String] = List.empty,
                         priceBreakdown: Option[PriceBreakdown] = None,
                         configuration: Option[ProductConfiguration] = None,
                         language: Language = Language.En,
                         basket: Basket = Basket(BasketId.unsafe("main-basket"), List.empty),
                         basketMessage: Option[String] = None,
                       )

object ProductBuilderViewModel:

  val catalog: ProductCatalog = SampleCatalog.catalog
  val ruleset = SampleRules.ruleset
  val pricelist = SamplePricelist.pricelistCzk

  val stateVar: Var[BuilderState] = Var(BuilderState())
  val state: Signal[BuilderState] = stateVar.signal

  // Event bus that fires when category changes, used to reset spec form fields
  val specResetBus: EventBus[Unit] = new EventBus[Unit]

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
    val componentStates = catalog.categories.get(categoryId) match
      case Some(cat) =>
        cat.components.map(ct => ct.role -> ComponentState(ct.role)).toMap
      case None =>
        Map.empty[ComponentRole, ComponentState]

    stateVar.update(state =>
      state.copy(
        selectedCategoryId = Some(categoryId),
        componentStates = componentStates,
        selectedPrintingMethodId = None,
        specifications = List.empty,
        validationErrors = List.empty,
        priceBreakdown = None,
        configuration = None,
      )
    )
    specResetBus.emit(())
    autoRecalculate()
  }

  // Update material selection for a specific component role
  def selectMaterial(role: ComponentRole, materialId: MaterialId): Unit = 
    stateVar.update(state =>
      val cs = state.componentStates.getOrElse(role, ComponentState(role))
      val updated = cs.copy(
        selectedMaterialId = Some(materialId),
        selectedFinishIds = Set.empty, // Reset finishes when material changes
      )
      state.copy(componentStates = state.componentStates + (role -> updated))
    )
    autoRecalculate()

  // Toggle finish selection for a specific component role
  def toggleFinish(role: ComponentRole, finishId: FinishId): Unit =
    stateVar.update(state =>
      val cs = state.componentStates.getOrElse(role, ComponentState(role))
      val newFinishIds =
        if cs.selectedFinishIds.contains(finishId) then
          cs.selectedFinishIds - finishId
        else
          cs.selectedFinishIds + finishId
      state.copy(componentStates = state.componentStates + (role -> cs.copy(selectedFinishIds = newFinishIds)))
    )
    autoRecalculate()

  // Select ink configuration for a specific component role
  def selectInkConfig(role: ComponentRole, config: InkConfiguration): Unit = 
    stateVar.update(state =>
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
            finishIds = cs.selectedFinishIds.toList,
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
                ))
              },
              breakdown => {
                stateVar.update(_.copy(
                  configuration = Some(config),
                  validationErrors = List.empty,
                  priceBreakdown = Some(breakdown),
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
        ))
  }

  // Get component roles for the currently selected category
  def componentRoles: Signal[List[ComponentRole]] =
    state.map(_.componentStates.keys.toList.sortBy(_.ordinal))

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
    state.map(_.componentStates.get(role).map(_.selectedFinishIds).getOrElse(Set.empty))

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
            stateVar.update(_.copy(
              basket = updatedBasket,
              basketMessage = Some(msg),
            ))
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
    stateVar.update(_.copy(basket = updatedBasket, basketMessage = None))

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
    stateVar.update(_.copy(basket = clearedBasket, basketMessage = None))

  def basketCalculation: Signal[BasketCalculation] =
    state.map(s => BasketService.calculateTotal(s.basket))

  def clearBasketMessage(): Unit =
    stateVar.update(_.copy(basketMessage = None))
