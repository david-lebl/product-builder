package mpbuilder.domain.sample

import mpbuilder.domain.catalog.FinishType
import mpbuilder.domain.rules.CompatibilityRule

/** Compatibility rules implied by the sample catalog (spec §4): one lamination
  * at a time, competing surface coatings are mutually exclusive, relief/foil
  * finishes need sufficiently thick stock, and creasing is capped at the 8
  * crease counts the pricelist knows.
  */
object SampleRules:

  import CompatibilityRule.*
  import FinishType.*

  private val surfaceCoatings = List(Lamination, UvCoating, AqueousCoating, SoftTouch)

  val rules: List[CompatibilityRule] =
    List(OnePerFinishType(Lamination))
      ++ (for
        (a, i) <- surfaceCoatings.zipWithIndex
        b      <- surfaceCoatings.drop(i + 1)
      yield MutuallyExclusiveFinishTypes(a, b))
      ++ List(
        FinishTypeMinWeight(Embossing, 250),
        FinishTypeMinWeight(Debossing, 250),
        FinishTypeMinWeight(FoilStamping, 250),
        MaxCreases(None, None, 8),
      )
