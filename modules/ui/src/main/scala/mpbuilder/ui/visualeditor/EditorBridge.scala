package mpbuilder.ui.visualeditor

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.ui.{AppRouter, AppRoute}

/** Bridge for passing product configuration context from the Product Builder to the Visual Editor. */
object EditorBridge:

  private val pendingContextVar: Var[Option[ProductContext]] = Var(None)
  val pendingContext: Signal[Option[ProductContext]] = pendingContextVar.signal

  /** Called by ProductBuilder when user clicks "Open Visual Editor".
    * Extracts relevant specs from the configuration and navigates to the editor.
    */
  def openEditorForProduct(config: ProductConfiguration, artworkId: ArtworkId): Unit =
    val ctx = extractProductContext(config)
    pendingContextVar.set(Some(ctx))
    AppRouter.navigateTo(AppRoute.VisualEditor(Some(artworkId.value)))

  /** Editor consumes the pending context on mount, then clears it. */
  def consumeContext(): Option[ProductContext] =
    val ctx = pendingContextVar.now()
    pendingContextVar.set(None)
    ctx

  private def extractProductContext(config: ProductConfiguration): ProductContext =
    val specs = config.specifications
    val dimension = specs.get(SpecKind.Size).collect { case SpecValue.SizeSpec(d) => d }
    val pageCount = specs.get(SpecKind.Pages).collect { case SpecValue.PagesSpec(c) => c }
    val bindingMethod = specs.get(SpecKind.BindingMethod).collect { case SpecValue.BindingMethodSpec(m) => m.toString }

    ProductContext(
      widthMm = dimension.map(_.widthMm).getOrElse(210.0),
      heightMm = dimension.map(_.heightMm).getOrElse(297.0),
      pageCount = pageCount,
      categoryId = Some(config.category.id.value),
      categoryName = Some(config.category.name(Language.En)),
      bindingMethod = bindingMethod,
      visualProductType = inferVisualProductType(config),
    )

  private def inferVisualProductType(config: ProductConfiguration): Option[VisualProductType] =
    config.category.id.value match
      case "cat-calendars" =>
        val binding = config.specifications.get(SpecKind.BindingMethod).collect {
          case SpecValue.BindingMethodSpec(m) => m
        }
        binding match
          case Some(BindingMethod.LoopBinding) =>
            Some(VisualProductType.MonthlyCalendar)
          case _ => Some(VisualProductType.MonthlyCalendar)
      case "cat-booklets" => Some(VisualProductType.PhotoBook)
      case "cat-roll-ups" => Some(VisualProductType.WallPicture)
      case "cat-posters"  => Some(VisualProductType.WallPicture)
      case _              => None // Will use GenericProduct
