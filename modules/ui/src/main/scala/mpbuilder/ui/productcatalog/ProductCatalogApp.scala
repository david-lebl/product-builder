package mpbuilder.ui.productcatalog

import com.raquo.laminar.api.L.*
import com.raquo.laminar.codecs.StringAsIsCodec
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.{Currency, Money, PriceBreakdown}
import mpbuilder.domain.sample.{SampleCatalog, SamplePricelist, SampleRules, SampleShowcase}
import mpbuilder.domain.service.PresetPriceService
import mpbuilder.ui.{AppRouter, AppRoute}
import mpbuilder.ui.productbuilder.ProductBuilderViewModel

/** Customer-facing product catalog with grid view and detail pages. */
object ProductCatalogApp:

  private val lazyLoading = htmlAttr("loading", StringAsIsCodec)

  private val allProducts = SampleShowcase.allProducts
  private val catalog = SampleCatalog.catalog

  /** Active catalog group filter. None = show all. */
  private val activeGroupVar: Var[Option[CatalogGroup]] = Var(None)

  /** The main catalog grid view. */
  def apply(): HtmlElement =
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "product-catalog",

      // Hero banner
      div(
        cls := "catalog-hero",
        div(
          cls := "catalog-hero-content",
          h1(child.text <-- lang.map {
            case Language.En => "Our Products"
            case Language.Cs => "Naše produkty"
          }),
          p(child.text <-- lang.map {
            case Language.En => "Premium print products crafted with care. Choose a product to explore options and start your order."
            case Language.Cs => "Prémiové tiskové produkty vyrobené s péčí. Vyberte produkt, prozkoumejte možnosti a zadejte objednávku."
          }),
        ),
      ),

      // Group filter tabs
      div(
        cls := "catalog-filters",
        groupFilterButton(None, lang),
        groupFilterButton(Some(CatalogGroup.Sheet), lang),
        groupFilterButton(Some(CatalogGroup.Bound), lang),
        groupFilterButton(Some(CatalogGroup.LargeFormat), lang),
        groupFilterButton(Some(CatalogGroup.Specialty), lang),
        groupFilterButton(Some(CatalogGroup.Promotional), lang),
      ),

      // Product grid
      div(
        cls := "catalog-grid",
        children <-- activeGroupVar.signal.combineWith(lang).map { (groupFilter: Option[CatalogGroup], l: Language) =>
          val filtered = groupFilter match
            case Some(g) => allProducts.filter(_.group == g)
            case None    => allProducts
          filtered.map(p => productCard(p, l))
        },
      ),
    )

  /** Product detail page for a given category ID. */
  def detailPage(categoryId: CategoryId): HtmlElement =
    val lang = ProductBuilderViewModel.currentLanguage

    SampleShowcase.forCategory(categoryId) match
      case None =>
        div(
          cls := "product-catalog",
          div(
            cls := "product-detail-not-found",
            h2("Product not found"),
            button(
              cls := "btn btn-primary",
              "← Back to Catalog",
              onClick --> { _ => AppRouter.navigateTo(AppRoute.ProductCatalog) },
            ),
          ),
        )
      case Some(product) =>
        val category = catalog.categories.get(product.categoryId)
        div(
          cls := "product-catalog",
          productDetailContent(product, category, lang),
        )

  // ── Private helpers ────────────────────────────────────────────────────

  private def groupFilterButton(group: Option[CatalogGroup], lang: Signal[Language]): HtmlElement =
    button(
      cls := "catalog-filter-btn",
      cls <-- activeGroupVar.signal.map(ag => if ag == group then "active" else ""),
      child.text <-- lang.map(l => groupLabel(group, l)),
      onClick --> { _ => activeGroupVar.set(group) },
    )

  private def groupLabel(group: Option[CatalogGroup], lang: Language): String = group match
    case None => lang match
      case Language.En => "All Products"
      case Language.Cs => "Všechny produkty"
    case Some(CatalogGroup.Sheet) => lang match
      case Language.En => "Sheet Products"
      case Language.Cs => "Tiskoviny"
    case Some(CatalogGroup.Bound) => lang match
      case Language.En => "Bound Products"
      case Language.Cs => "Vázané produkty"
    case Some(CatalogGroup.LargeFormat) => lang match
      case Language.En => "Large Format"
      case Language.Cs => "Velkoformát"
    case Some(CatalogGroup.Specialty) => lang match
      case Language.En => "Specialty"
      case Language.Cs => "Speciální"
    case Some(CatalogGroup.Promotional) => lang match
      case Language.En => "Promotional"
      case Language.Cs => "Reklamní"

  private def groupBadgeLabel(group: CatalogGroup, lang: Language): String = group match
    case CatalogGroup.Sheet => lang match
      case Language.En => "Sheet"
      case Language.Cs => "Tisk"
    case CatalogGroup.Bound => lang match
      case Language.En => "Bound"
      case Language.Cs => "Vázané"
    case CatalogGroup.LargeFormat => lang match
      case Language.En => "Large Format"
      case Language.Cs => "Velkoformát"
    case CatalogGroup.Specialty => lang match
      case Language.En => "Specialty"
      case Language.Cs => "Speciální"
    case CatalogGroup.Promotional => lang match
      case Language.En => "Promo"
      case Language.Cs => "Reklamní"

  /** A single product card in the catalog grid. */
  private def productCard(product: ShowcaseProduct, lang: Language): HtmlElement =
    val category = catalog.categories.get(product.categoryId)
    val name = category.map(_.name(lang)).getOrElse("Unknown")

    div(
      cls := "catalog-card",
      onClick --> { _ => AppRouter.navigateTo(AppRoute.ProductDetail(product.categoryId)) },

      // Image
      div(
        cls := "catalog-card-image",
        img(
          src := product.imageUrl,
          alt := name,
          lazyLoading := "lazy",
        ),
        // Group badge
        span(cls := "catalog-card-badge", groupBadgeLabel(product.group, lang)),
      ),

      // Content
      div(
        cls := "catalog-card-content",
        h3(cls := "catalog-card-title", name),
        p(cls := "catalog-card-tagline", product.tagline(lang)),

        // Quick info row
        div(
          cls := "catalog-card-meta",
          product.turnaroundDays.map(days =>
            span(cls := "catalog-card-meta-item", s"⏱ $days ", lang match
              case Language.En => "days"
              case Language.Cs => "dní"
            )
          ).getOrElse(emptyNode),
          if product.variations.nonEmpty then
            span(cls := "catalog-card-meta-item", s"📋 ${product.variations.size} ", lang match
              case Language.En => "variations"
              case Language.Cs => "variant"
            )
          else emptyNode,
        ),
      ),
    )

  /** Full detail page content for a showcase product. */
  private def productDetailContent(
      product: ShowcaseProduct,
      category: Option[ProductCategory],
      lang: Signal[Language],
  ): HtmlElement =
    val name = category.map(_.name).getOrElse(LocalizedString("Product"))

    // Pre-compute prices for all variations that have presetIds
    val variationPrices: Map[PresetId, PriceBreakdown] = category match
      case Some(cat) =>
        product.variations.flatMap { v =>
          v.presetId.flatMap { pid =>
            cat.presetById(pid).flatMap { preset =>
              PresetPriceService.computePrice(cat, preset, catalog, ProductBuilderViewModel.ruleset, ProductBuilderViewModel.pricelist)
                .map(pid -> _)
            }
          }
        }.toMap
      case None => Map.empty

    div(
      cls := "product-detail",

      // Back navigation
      div(
        cls := "product-detail-back",
        button(
          cls := "btn btn-link",
          child.text <-- lang.map {
            case Language.En => "← Back to Products"
            case Language.Cs => "← Zpět na produkty"
          },
          onClick --> { _ => AppRouter.navigateTo(AppRoute.ProductCatalog) },
        ),
      ),

      // Hero section: image + basic info
      div(
        cls := "product-detail-hero",

        // Image gallery
        div(
          cls := "product-detail-gallery",
          img(
            cls := "product-detail-main-image",
            src := product.imageUrl,
            alt <-- lang.map(l => name(l)),
          ),
          if product.galleryImageUrls.nonEmpty then
            div(
              cls := "product-detail-thumbnails",
              product.galleryImageUrls.map { url =>
                img(
                  cls := "product-detail-thumb",
                  src := url,
                  alt := "Gallery image",
                  lazyLoading := "lazy",
                )
              },
            )
          else emptyNode,
        ),

        // Info column
        div(
          cls := "product-detail-info",
          span(
            cls := "product-detail-group-badge",
            child.text <-- lang.map(l => groupBadgeLabel(product.group, l)),
          ),
          h1(child.text <-- lang.map(l => name(l))),
          p(cls := "product-detail-tagline", child.text <-- lang.map(l => product.tagline(l))),
          div(
            cls := "product-detail-description",
            children <-- lang.map { l =>
              val text = product.detailedDescription(l)
              val paragraphs = text.split("\n\n").toList.filter(_.nonEmpty)
              paragraphs.map(para => p(para))
            },
          ),

          // Turnaround info
          product.turnaroundDays.map { days =>
            div(
              cls := "product-detail-turnaround",
              span(cls := "turnaround-icon", "⏱"),
              span(child.text <-- lang.map {
                case Language.En => s"Typical turnaround: $days business days"
                case Language.Cs => s"Typická doba výroby: $days pracovních dní"
              }),
            )
          }.getOrElse(emptyNode),

          // CTA button
          div(
            cls := "product-detail-cta",
            button(
              cls := "btn btn-primary btn-lg",
              child.text <-- lang.map {
                case Language.En => "Configure & Order →"
                case Language.Cs => "Konfigurovat a objednat →"
              },
              onClick --> { _ =>
                ProductBuilderViewModel.selectCategory(product.categoryId)
                AppRouter.navigateTo(AppRoute.ProductBuilder)
              },
            ),
          ),

          // Available Variations — placed right after CTA
          if product.variations.nonEmpty then
            div(
              cls := "product-variations-section",
              h3(child.text <-- lang.map {
                case Language.En => "Available Variations"
                case Language.Cs => "Dostupné varianty"
              }),
              div(
                cls := "product-variations-grid",
                product.variations.map { v =>
                  variationCard(product.categoryId, v, variationPrices, lang)
                },
              ),
            )
          else emptyNode,

          // Popular finishes
          if product.popularFinishes.nonEmpty then
            div(
              cls := "product-detail-popular-finishes",
              h4(child.text <-- lang.map {
                case Language.En => "Popular Finishes"
                case Language.Cs => "Oblíbené úpravy"
              }),
              div(
                cls := "finish-tags",
                product.popularFinishes.map(f => span(cls := "finish-tag", f)),
              ),
            )
          else emptyNode,
        ),
      ),

      // Features section
      if product.features.nonEmpty then
        div(
          cls := "product-detail-section",
          h2(child.text <-- lang.map {
            case Language.En => "Key Features"
            case Language.Cs => "Klíčové vlastnosti"
          }),
          div(
            cls := "product-features-grid",
            product.features.map { f =>
              div(
                cls := "product-feature-card",
                span(cls := "feature-icon", f.icon),
                h4(child.text <-- lang.map(l => f.title(l))),
                p(child.text <-- lang.map(l => f.description(l))),
              )
            },
          ),
        )
      else emptyNode,

      // Instructions section
      product.instructions.map { instr =>
        div(
          cls := "product-detail-section",
          h2(child.text <-- lang.map {
            case Language.En => "How to Order"
            case Language.Cs => "Jak objednat"
          }),
          div(
            cls := "product-instructions",
            child.text <-- lang.map(l => instr(l)),
          ),
        )
      }.getOrElse(emptyNode),

      // Materials info (from catalog)
      category.map { cat =>
        val materials = cat.allAllowedMaterialIds.flatMap(id => catalog.materials.get(id)).toList
        if materials.nonEmpty then
          div(
            cls := "product-detail-section",
            h2(child.text <-- lang.map {
              case Language.En => "Available Materials"
              case Language.Cs => "Dostupné materiály"
            }),
            div(
              cls := "product-materials-list",
              materials.map { m =>
                div(
                  cls := "material-chip",
                  child.text <-- lang.map(l => m.name(l)),
                )
              },
            ),
          )
        else emptyNode
      }.getOrElse(emptyNode),

      // Bottom CTA
      div(
        cls := "product-detail-bottom-cta",
        button(
          cls := "btn btn-primary btn-lg",
          child.text <-- lang.map {
            case Language.En => "Start Configuring This Product →"
            case Language.Cs => "Začít konfigurovat tento produkt →"
          },
          onClick --> { _ =>
            ProductBuilderViewModel.selectCategory(product.categoryId)
            AppRouter.navigateTo(AppRoute.ProductBuilder)
          },
        ),
      ),
    )

  /** A clickable variation card that navigates to the builder with the linked preset. */
  private def variationCard(
      categoryId: CategoryId,
      variation: ProductVariation,
      prices: Map[PresetId, PriceBreakdown],
      lang: Signal[Language],
  ): HtmlElement =
    val isClickable = variation.presetId.isDefined
    val priceOpt = variation.presetId.flatMap(prices.get)
    val currency = ProductBuilderViewModel.pricelist.currency

    div(
      cls := (if isClickable then "product-variation-card product-variation-card-clickable" else "product-variation-card"),
      h4(child.text <-- lang.map(l => variation.name(l))),
      p(child.text <-- lang.map(l => variation.description(l))),
      priceOpt.map { breakdown =>
        div(
          cls := "variation-price-tag",
          child.text <-- lang.map {
            case Language.En => s"from ${formatMoney(breakdown.total, currency)}"
            case Language.Cs => s"od ${formatMoney(breakdown.total, currency)}"
          },
        )
      }.getOrElse(emptyNode),
      if isClickable then
        onClick --> { _ =>
          variation.presetId.foreach { pid =>
            ProductBuilderViewModel.selectCategoryWithPreset(categoryId, pid)
            AppRouter.navigateTo(AppRoute.ProductBuilder)
          }
        }
      else emptyMod,
    )

  private def formatMoney(money: Money, currency: Currency): String =
    val value = money.value.setScale(0, BigDecimal.RoundingMode.HALF_UP)
    s"$value ${currency.toString}"
