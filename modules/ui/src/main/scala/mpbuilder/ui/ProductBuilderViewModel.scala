package mpbuilder.ui

import mpbuilder.domain.model.*
import mpbuilder.domain.service.*
import mpbuilder.domain.validation.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.sample.*
import zio.prelude.Validation
import com.raquo.laminar.api.L.*
import org.scalajs.dom

/** Application state for the product builder */
case class BuilderState(
  selectedCategoryId: Option[CategoryId] = None,
  selectedMaterialId: Option[MaterialId] = None,
  selectedFinishIds: Set[FinishId] = Set.empty,
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
  
  // Update category selection
  def selectCategory(categoryId: CategoryId): Unit =
    stateVar.update(state =>
      state.copy(
        selectedCategoryId = Some(categoryId),
        // Reset dependent selections
        selectedMaterialId = None,
        selectedFinishIds = Set.empty,
        selectedPrintingMethodId = None,
        specifications = List.empty,
        validationErrors = List.empty,
        priceBreakdown = None,
        configuration = None,
      )
    )
    specResetBus.emit(())
  
  // Update material selection
  def selectMaterial(materialId: MaterialId): Unit =
    stateVar.update(state =>
      state.copy(
        selectedMaterialId = Some(materialId),
        // Reset dependent selections
        selectedFinishIds = Set.empty,
      )
    )
  
  // Toggle finish selection
  def toggleFinish(finishId: FinishId): Unit =
    stateVar.update(state =>
      val newFinishIds = 
        if state.selectedFinishIds.contains(finishId) then
          state.selectedFinishIds - finishId
        else
          state.selectedFinishIds + finishId
      
      state.copy(selectedFinishIds = newFinishIds)
    )
  
  // Update printing method
  def selectPrintingMethod(methodId: PrintingMethodId): Unit =
    stateVar.update(state =>
      state.copy(selectedPrintingMethodId = Some(methodId))
    )
  
  // Update specifications
  def updateSpecifications(specs: List[SpecValue]): Unit =
    stateVar.update(state =>
      state.copy(specifications = specs)
    )
  
  // Add a specification
  def addSpecification(spec: SpecValue): Unit =
    stateVar.update(state =>
      state.copy(specifications = state.specifications :+ spec)
    )
  
  // Remove a specification by type
  def removeSpecification(specType: Class[?]): Unit =
    stateVar.update(state =>
      state.copy(specifications = state.specifications.filterNot(s => s.getClass == specType))
    )
  
  // Build and validate configuration
  def validateConfiguration(): Unit =
    val currentState = stateVar.now()
    val lang = currentState.language
    
    (currentState.selectedCategoryId, currentState.selectedMaterialId, currentState.selectedPrintingMethodId) match
      case (Some(categoryId), Some(materialId), Some(printingMethodId)) =>
        // Build configuration request
        val request = ConfigurationRequest(
          categoryId = categoryId,
          materialId = materialId,
          printingMethodId = printingMethodId,
          finishIds = currentState.selectedFinishIds.toList,
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
          case Language.En => "Please select a category, material, and printing method"
          case Language.Cs => "Vyberte prosím kategorii, materiál a tiskovou metodu"
        stateVar.update(_.copy(
          validationErrors = List(msg),
          configuration = None,
          priceBreakdown = None,
        ))
  
  // Get available materials for selected category
  def availableMaterials: Signal[List[Material]] =
    state.map { s =>
      s.selectedCategoryId match
        case Some(categoryId) =>
          CatalogQueryService.availableMaterials(categoryId, catalog)
        case None =>
          List.empty
    }
  
  // Get available finishes for selected material and category
  def availableFinishes: Signal[List[Finish]] =
    state.map { s =>
      (s.selectedCategoryId, s.selectedMaterialId) match
        case (Some(categoryId), Some(materialId)) =>
          CatalogQueryService.compatibleFinishes(
            categoryId,
            materialId,
            catalog,
            ruleset,
            s.selectedPrintingMethodId,
          )
        case _ =>
          List.empty
    }
  
  // Get required spec kinds for the selected category
  def requiredSpecKinds: Signal[Set[SpecKind]] =
    state.map { s =>
      s.selectedCategoryId.flatMap(id => catalog.categories.get(id)) match
        case Some(cat) => cat.requiredSpecKinds
        case None      => Set.empty
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

  // Get currently selected specifications as signals for UI binding
  def selectedInkConfig: Signal[Option[InkConfiguration]] =
    state.map { s =>
      s.specifications.collectFirst {
        case SpecValue.InkConfigSpec(config) => config
      }
    }

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
