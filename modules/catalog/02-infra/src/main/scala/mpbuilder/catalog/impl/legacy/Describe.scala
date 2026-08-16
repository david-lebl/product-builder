package mpbuilder.catalog
package impl
package legacy

import mpbuilder.commons.*
import mpbuilder.domain.model as dm

/** Renders a configuration as one human-readable line.
  *
  * Called once, when an order is placed, and the result stored on the order line — so what the
  * customer saw on their confirmation stays readable even after the catalog has moved on.
  */
private[catalog] object Describe:

  def apply(config: dm.ProductConfiguration, lang: Language): String =
    val parts = List(
      quantity(config),
      Some(config.category.name(lang)),
      size(config),
      Some(config.printingMethod.name(lang)),
      materials(config, lang),
      finishes(config, lang),
    ).flatten
    parts.mkString(", ")

  private def quantity(config: dm.ProductConfiguration): Option[String] =
    config.specifications.get(dm.SpecKind.Quantity).collect { case dm.SpecValue.QuantitySpec(q) =>
      s"${q.value}×"
    }

  private def size(config: dm.ProductConfiguration): Option[String] =
    config.specifications.get(dm.SpecKind.Size).collect { case dm.SpecValue.SizeSpec(d) =>
      s"${trim(d.widthMm)}×${trim(d.heightMm)} mm"
    }

  private def materials(config: dm.ProductConfiguration, lang: Language): Option[String] =
    config.components.map(_.material.name(lang)).distinct match
      case Nil    => None
      case values => Some(values.mkString(" + "))

  private def finishes(config: dm.ProductConfiguration, lang: Language): Option[String] =
    config.components.flatMap(_.finishes.map(_.finish.name(lang))).distinct match
      case Nil    => None
      case values => Some(values.mkString(", "))

  private def trim(value: Double): String =
    if value == value.floor then value.toInt.toString else value.toString
