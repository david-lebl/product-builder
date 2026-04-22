package mpbuilder.domain.service

import mpbuilder.domain.model.*
import mpbuilder.domain.manufacturing.TierRestriction
import mpbuilder.domain.rules.CompatibilityRule

object TierRestrictionValidator:
  final case class TierViolation(
      tier: ManufacturingSpeed,
      reason: String,
      reasonCs: String,
  ):
    def message(lang: Language): String = lang match
      case Language.En => reason
      case Language.Cs => reasonCs

  def validate(
      tier: ManufacturingSpeed,
      restrictions: List[TierRestriction],
      categoryId: CategoryId,
      quantity: Int,
      bindingMethod: Option[BindingMethod],
      finishTypes: Set[FinishType],
      materialIds: Set[MaterialId],
      matchingExternalRules: List[CompatibilityRule.RequiresExternalPartner] = Nil,
  ): List[TierViolation] =
    val externalViolations =
      if matchingExternalRules.nonEmpty && tier != ManufacturingSpeed.Standard then
        val tierEn = tier match { case ManufacturingSpeed.Express => "Express"; case ManufacturingSpeed.Standard => "Standard"; case ManufacturingSpeed.Economy => "Economy" }
        val tierCs = tier match { case ManufacturingSpeed.Express => "Expres"; case ManufacturingSpeed.Standard => "Standardní"; case ManufacturingSpeed.Economy => "Ekonomická" }
        List(TierViolation(tier,
          s"$tierEn is not available for external production — Standard turnaround only",
          s"$tierCs není dostupná pro externí výrobu — pouze standardní termín",
        ))
      else Nil

    val applicableRestrictions = restrictions.filter(r => r.categoryId == categoryId && r.tier == tier)
    val categoryViolations = applicableRestrictions.flatMap { r =>
      val violations = List.newBuilder[TierViolation]
      r.maxQuantity.foreach { max =>
        if quantity > max then
          val tierEn = tier match { case ManufacturingSpeed.Express => "Express"; case ManufacturingSpeed.Standard => "Standard"; case ManufacturingSpeed.Economy => "Economy" }
          val tierCs = tier match { case ManufacturingSpeed.Express => "Expres"; case ManufacturingSpeed.Standard => "Standardní"; case ManufacturingSpeed.Economy => "Ekonomickou" }
          violations += TierViolation(tier, s"Max quantity for $tierEn is $max", s"Max. množství pro $tierCs je $max")
      }
      // binding method check (perfect binding has cure time)
      bindingMethod.foreach {
        case BindingMethod.PerfectBinding if tier == ManufacturingSpeed.Express =>
          violations += TierViolation(tier, "Express not available with perfect binding (glue cure time)", "Expres není dostupný s lepenou vazbou (čas tuhnutí lepidla)")
        case BindingMethod.CaseBinding if tier == ManufacturingSpeed.Express =>
          violations += TierViolation(tier, "Express not available with case binding (drying time)", "Expres není dostupný s tuhou vazbou (čas schnutí)")
        case _ => ()
      }
      // blocked materials check
      r.blockedMaterials.foreach { blocked =>
        val found = materialIds.intersect(blocked)
        if found.nonEmpty then
          val tierEn = tier match { case ManufacturingSpeed.Express => "Express"; case ManufacturingSpeed.Standard => "Standard"; case ManufacturingSpeed.Economy => "Economy" }
          val tierCs = tier match { case ManufacturingSpeed.Express => "Expres"; case ManufacturingSpeed.Standard => "Standardní"; case ManufacturingSpeed.Economy => "Ekonomickou" }
          violations += TierViolation(tier, s"Some materials are not available for $tierEn", s"Některé materiály nejsou dostupné pro $tierCs")
      }
      violations.result()
    }

    externalViolations ::: categoryViolations
