package mpbuilder.ui

import mpbuilder.domain.model.*
import mpbuilder.domain.service.*
import mpbuilder.domain.validation.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.sample.*
import zio.prelude.Validation
import com.raquo.laminar.api.L.*

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
)

object ProductBuilderViewModel:
  
  val catalog: ProductCatalog = SampleCatalog.catalog
  val ruleset = SampleRules.ruleset
  val pricelist = SamplePricelist.pricelist
  
  val stateVar: Var[BuilderState] = Var(BuilderState())
  val state: Signal[BuilderState] = stateVar.signal
  
  // Get all categories as a list
  def allCategories: List[ProductCategory] = catalog.categories.values.toList
  
  // Update category selection
  def selectCategory(categoryId: CategoryId): Unit =
    val newState = BuilderState(selectedCategoryId = Some(categoryId))
    stateVar.set(newState)
  
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
            val errorMessages = errors.map(_.message).toList
            stateVar.update(_.copy(
              configuration = None,
              validationErrors = errorMessages,
              priceBreakdown = None,
            ))
          },
          config => {
            // Validation succeeded, calculate price
            val priceResult = PriceCalculator.calculate(config, pricelist)
            priceResult.fold(
              errors => {
                val errorMessages = errors.map(_.message).toList
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
        stateVar.update(_.copy(
          validationErrors = List("Please select a category, material, and printing method"),
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
