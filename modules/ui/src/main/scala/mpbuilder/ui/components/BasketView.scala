package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.{ProductBuilderViewModel, ArtworkMode}
import mpbuilder.domain.pricing.{Money, Currency}
import mpbuilder.domain.model.{Language, ConfigurationId, ComponentRole}

object BasketView:
  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage
    val basketCalc = ProductBuilderViewModel.basketCalculation

    div(
      cls := "card",
      h2(child.text <-- lang.map {
        case Language.En => "Shopping Basket"
        case Language.Cs => "Nákupní košík"
      }),

      // Basket message (success or error)
      div(
        child.maybe <-- ProductBuilderViewModel.state.map { state =>
          state.basketMessage.map { msg =>
            div(
              cls := "info-box",
              msg,
              button(
                cls := "close-msg-btn",
                "×",
                onClick --> { _ =>
                  ProductBuilderViewModel.clearBasketMessage()
                },
              ),
            )
          }
        },
      ),

      // Basket items
      div(
        cls := "basket-items",
        children <-- ProductBuilderViewModel.state.combineWith(lang).map { case (state, l) =>
          if state.basket.items.isEmpty then
            List(
              p(
                cls := "empty-basket",
                l match
                  case Language.En => "Your basket is empty. Configure a product and click 'Add to Basket'."
                  case Language.Cs => "Váš košík je prázdný. Nakonfigurujte produkt a klikněte na 'Přidat do košíku'."
              ),
            )
          else
            state.basket.items.map { item =>
              div(
                cls := "basket-item",
                div(
                  cls := "basket-item-header",
                  div(
                    cls := "basket-item-title",
                    strong(item.configuration.category.name(l)),
                    span(cls := "basket-item-material", s" • ${item.configuration.components.map(_.material.name(l)).mkString(", ")}"),
                  ),
                  button(
                    cls := "remove-btn",
                    "×",
                    title := (l match
                      case Language.En => "Remove from basket"
                      case Language.Cs => "Odebrat z košíku"
                    ),
                    onClick --> { _ =>
                      ProductBuilderViewModel.removeFromBasket(item.configuration.id)
                    },
                  ),
                ),
                div(
                  cls := "basket-item-details",
                  div(
                    cls := "basket-item-specs",
                    span(l match
                      case Language.En => s"Method: ${item.configuration.printingMethod.name(l)}"
                      case Language.Cs => s"Metoda: ${item.configuration.printingMethod.name(l)}"
                    ),
                    {
                      val allFinishes = item.configuration.components.flatMap(_.finishes)
                      if allFinishes.nonEmpty then
                        span(
                          cls := "basket-item-finishes",
                          l match
                            case Language.En => s" | Finishes: ${allFinishes.map(sf => finishDescription(sf, l)).mkString(", ")}"
                            case Language.Cs => s" | Povrchové úpravy: ${allFinishes.map(sf => finishDescription(sf, l)).mkString(", ")}"
                        )
                      else emptyNode
                    },
                  ),
                  div(
                    cls := "basket-item-quantity",
                    label(l match
                      case Language.En => "Quantity: "
                      case Language.Cs => "Množství: "
                    ),
                    input(
                      typ := "number",
                      minAttr := "1",
                      value := item.quantity.toString,
                      cls := "quantity-input",
                      onInput.mapToValue.map(_.toIntOption.getOrElse(1)) --> { newQty =>
                        ProductBuilderViewModel.updateBasketQuantity(item.configuration.id, newQty)
                      },
                    ),
                  ),
                  div(
                    cls := "basket-item-price",
                    span(cls := "unit-price", l match
                      case Language.En => s"Unit: ${formatMoney(item.priceBreakdown.total, item.priceBreakdown.currency)}"
                      case Language.Cs => s"Jednotka: ${formatMoney(item.priceBreakdown.total, item.priceBreakdown.currency)}"
                    ),
                    span(cls := "line-total", l match
                      case Language.En => s"Total: ${formatMoney(item.priceBreakdown.total * item.quantity, item.priceBreakdown.currency)}"
                      case Language.Cs => s"Celkem: ${formatMoney(item.priceBreakdown.total * item.quantity, item.priceBreakdown.currency)}"
                    ),
                  ),
                  div(
                    cls := "basket-item-artwork",
                    state.basketItemArtwork.get(item.configuration.id) match
                      case Some(ArtworkMode.UploadArtwork(Some(fileName))) =>
                        span(l match
                          case Language.En => s"📎 Artwork: $fileName"
                          case Language.Cs => s"📎 Data: $fileName"
                        )
                      case Some(ArtworkMode.UploadArtwork(None)) =>
                        span(cls := "artwork-pending", l match
                          case Language.En => "📎 Artwork: not uploaded yet"
                          case Language.Cs => "📎 Data: ještě nenahrána"
                        )
                      case Some(ArtworkMode.DesignInEditor) =>
                        span(l match
                          case Language.En => "🎨 Design: created in Visual Editor"
                          case Language.Cs => "🎨 Design: vytvořen ve vizuálním editoru"
                        )
                      case None => emptyNode
                  ),
                ),
              )
            }
        },
      ),

      // Basket total
      div(
        cls := "basket-total",
        child.maybe <-- ProductBuilderViewModel.state.combineWith(lang).map { case (state, l) =>
          if state.basket.items.nonEmpty then
            Some(
              div(
                cls := "basket-total-row",
                strong(l match
                  case Language.En => "Basket Total:"
                  case Language.Cs => "Celkem v košíku:"
                ),
                strong(child.text <-- basketCalc.map(calc => formatMoney(calc.total, calc.currency))),
              )
            )
          else
            None
        },
      ),

      // Basket actions
      div(
        cls := "basket-actions",
        child.maybe <-- ProductBuilderViewModel.state.combineWith(lang).map { case (state, l) =>
          if state.basket.items.nonEmpty then
            Some(
              button(
                cls := "clear-basket-btn",
                l match
                  case Language.En => "Clear Basket"
                  case Language.Cs => "Vyprázdnit košík"
                ,
                onClick --> { _ =>
                  ProductBuilderViewModel.clearBasket()
                },
              )
            )
          else
            None
        },
      ),
    )

  private def formatMoney(money: Money, currency: Currency): String =
    currency match
      case Currency.CZK => f"${money.value}%.2f Kč"
      case Currency.USD => f"$$${money.value}%.2f"
      case Currency.EUR => f"€${money.value}%.2f"
      case Currency.GBP => f"£${money.value}%.2f"

  private def finishDescription(sf: mpbuilder.domain.model.SelectedFinish, lang: Language): String =
    import mpbuilder.domain.model.FinishParameters.*
    import mpbuilder.domain.model.FoilColor.*
    import mpbuilder.domain.model.FinishSide.*
    val paramsDesc = sf.params match
      case None => ""
      case Some(RoundCornersParams(count, radius)) => lang match
        case Language.En => s" ($count corners, ${radius}mm radius)"
        case Language.Cs => s" ($count rohů, ${radius}mm poloměr)"
      case Some(LaminationParams(Front)) => lang match
        case Language.En => " (front only)"
        case Language.Cs => " (pouze přední)"
      case Some(LaminationParams(_)) => lang match
        case Language.En => " (both sides)"
        case Language.Cs => " (obě strany)"
      case Some(FoilStampingParams(Gold))        => lang match { case Language.En => " (gold)";        case Language.Cs => " (zlatá)" }
      case Some(FoilStampingParams(Silver))      => lang match { case Language.En => " (silver)";      case Language.Cs => " (stříbrná)" }
      case Some(FoilStampingParams(Copper))      => lang match { case Language.En => " (copper)";      case Language.Cs => " (měděná)" }
      case Some(FoilStampingParams(RoseGold))    => lang match { case Language.En => " (rose gold)";   case Language.Cs => " (růžové zlato)" }
      case Some(FoilStampingParams(Holographic)) => lang match { case Language.En => " (holographic)"; case Language.Cs => " (holografická)" }
      case Some(GrommetParams(spacing)) => lang match
        case Language.En => s" (${spacing}mm spacing)"
        case Language.Cs => s" (rozteč ${spacing}mm)"
      case Some(PerforationParams(pitch)) => lang match
        case Language.En => s" (${pitch}mm pitch)"
        case Language.Cs => s" (rozteč ${pitch}mm)"
    sf.name(lang) + paramsDesc
