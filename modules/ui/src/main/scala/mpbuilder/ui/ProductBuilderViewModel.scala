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
  specifications: List[ProductSpecification] = List.empty,
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
  def updateSpecifications(specs: List[ProductSpecification]): Unit =
    stateVar.update(state =>
      state.copy(specifications = specs)
    )
  
  // Add a specification
  def addSpecification(spec: ProductSpecification): Unit =
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
    
    (currentState.selectedCategoryId, currentState.selectedMaterialId) match
      case (Some(categoryId), Some(materialId)) =>
        // Get category and material from catalog
        val categoryOpt = catalog.categories.find(_.id == categoryId)
        val materialOpt = catalog.materials.find(_.id == materialId)
        val finishes = currentState.selectedFinishIds.flatMap(fId => 
          catalog.finishes.find(_.id == fId)
        ).toList
        
        (categoryOpt, materialOpt) match
          case (Some(category), Some(material)) =>
            // Build configuration request
            val request = ConfigurationRequest(
              categoryId = categoryId,
              materialId = materialId,
              finishIds = currentState.selectedFinishIds.toList,
              printingMethodId = currentState.selectedPrintingMethodId,
              specifications = currentState.specifications,
            )
            
            // Validate
            val result = ConfigurationBuilder.build(catalog, ruleset)(request)
            
            result match
              case Validation.Success(config) =>
                // Calculate price
                val priceResult = PriceCalculator.calculate(pricelist)(config)
                priceResult match
                  case Validation.Success(breakdown) =>
                    stateVar.update(_.copy(
                      configuration = Some(config),
                      validationErrors = List.empty,
                      priceBreakdown = Some(breakdown),
                    ))
                  case Validation.Failure(errors) =>
                    val errorMessages = errors.map(_.message).toList
                    stateVar.update(_.copy(
                      configuration = Some(config),
                      validationErrors = errorMessages,
                      priceBreakdown = None,
                    ))
              case Validation.Failure(errors) =>
                val errorMessages = errors.map(_.message).toList
                stateVar.update(_.copy(
                  configuration = None,
                  validationErrors = errorMessages,
                  priceBreakdown = None,
                ))
          case _ =>
            stateVar.update(_.copy(
              validationErrors = List("Invalid category or material selection"),
              configuration = None,
              priceBreakdown = None,
            ))
      case _ =>
        stateVar.update(_.copy(
          validationErrors = List("Please select a category and material"),
          configuration = None,
          priceBreakdown = None,
        ))
  
  // Get available materials for selected category
  def availableMaterials: Signal[List[Material]] =
    state.map { s =>
      s.selectedCategoryId match
        case Some(categoryId) =>
          CatalogQueryService.availableMaterials(catalog, categoryId)
        case None =>
          List.empty
    }
  
  // Get available finishes for selected material and category
  def availableFinishes: Signal[List[Finish]] =
    state.map { s =>
      (s.selectedCategoryId, s.selectedMaterialId) match
        case (Some(categoryId), Some(materialId)) =>
          CatalogQueryService.compatibleFinishes(
            catalog,
            ruleset,
            categoryId,
            materialId,
            s.selectedPrintingMethodId,
          )
        case _ =>
          List.empty
    }
  
  // Get available printing methods for selected category
  def availablePrintingMethods: Signal[List[PrintingMethod]] =
    state.map { s =>
      s.selectedCategoryId match
        case Some(categoryId) =>
          val category = catalog.categories.find(_.id == categoryId)
          category match
            case Some(cat) =>
              if cat.allowedPrintingMethodIds.isEmpty then
                catalog.printingMethods
              else
                catalog.printingMethods.filter(pm => cat.allowedPrintingMethodIds.contains(pm.id))
            case None =>
              List.empty
        case None =>
          List.empty
    }
