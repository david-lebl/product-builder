package mpbuilder.catalog
package core

import mpbuilder.shared.*

/** Pure error-accumulating product configurator.
  *
  * Resolves catalog references, validates structural membership, finish side policies,
  * finish parameter schemas, per-component printing capabilities, stock imposition,
  * and compatibility rules — returning ALL errors together via `Validation`.
  */
private[catalog] object ConfigurationEngine:

  def configure(
      cmd: ConfigureProductCmd,
      catalog: ProductCatalog,
      ruleset: CompatibilityRuleset,
  ): Validation[CatalogError, ConfigurationResult] =
    val categoryV: Validation[CatalogError, ProductCategory] =
      catalog.categories.get(cmd.categoryId) match
        case Some(c) if c.enabled => Validation.succeed(c)
        case Some(_)              => Validation.fail(CatalogError.CategoryDisabled(cmd.categoryId))
        case None                 => Validation.fail(CatalogError.CategoryNotFound(cmd.categoryId))

    categoryV.flatMap { category =>
      val duplicateCheck = checkNoDuplicates(cmd.components)
      val roleCheck      = checkComponentRoles(category, cmd.components)
      val specReqCheck   = checkSpecRequirements(category, cmd.specs)

      val resolvedComponentsV: Validation[CatalogError, NonEmptyChunk[ConfiguredComponent]] =
        resolveComponents(category, cmd.components, cmd.specs, catalog)

      Validation
        .validateWith(duplicateCheck, roleCheck, specReqCheck, resolvedComponentsV)((_, _, _, comps) => comps)
        .flatMap { configuredComponents =>
          val outcome = RuleEngine.evaluateAll(ruleset, category.id, configuredComponents.toList, cmd.specs)
          outcome.toValidation.map { warnings =>
            val weightSummary = GeometryResolver.computeWeight(configuredComponents, cmd.specs.quantity)
            val product = ConfiguredProduct(
              id = cmd.configurationId,
              catalogVersion = catalog.version,
              rulesetVersion = ruleset.version,
              categoryId = category.id,
              categoryName = category.name,
              components = configuredComponents,
              specs = cmd.specs,
              weight = weightSummary,
            )
            ConfigurationResult(product = product, warnings = warnings)
          }
        }
    }

  private def checkNoDuplicates(selections: NonEmptyChunk[ComponentSelection]): Validation[CatalogError, Unit] =
    val duplicates = selections.toList.groupBy(_.key).collect { case (k, list) if list.size > 1 => k }.toList
    combineUnit(duplicates.map(k => Validation.fail(CatalogError.DuplicateComponent(k))))

  private def checkComponentRoles(
      category: ProductCategory,
      selections: NonEmptyChunk[ComponentSelection],
  ): Validation[CatalogError, Unit] =
    val expectedKeys = category.components.map(_.key).toSet
    val requiredKeys = category.components.filter(_.required).map(_.key).toSet
    val actualKeys   = selections.map(_.key).toSet

    val missing = (requiredKeys -- actualKeys).toList.map(k =>
      Validation.fail(CatalogError.MissingRequiredComponent(category.id, k))
    )
    val unexpected = (actualKeys -- expectedKeys).toList.map(k =>
      Validation.fail(CatalogError.UnexpectedComponent(category.id, k))
    )
    combineUnit(missing ++ unexpected)

  private def checkSpecRequirements(
      category: ProductCategory,
      specs: ProductSpecs,
  ): Validation[CatalogError, Unit] =
    val reqs = category.specRequirements
    val checks = List(
      if reqs.requiresFoldedDimensions && specs.dimensions.foldTypeOpt.isEmpty then
        Validation.fail(CatalogError.MissingRequiredSpecification(category.id, "Folded2D dimensions"))
      else Validation.unit,
      if reqs.requiresBinding && specs.binding.isEmpty then
        Validation.fail(CatalogError.MissingRequiredSpecification(category.id, "BindingSpec"))
      else Validation.unit,
      if reqs.requiresOrientation && specs.orientation.isEmpty then
        Validation.fail(CatalogError.MissingRequiredSpecification(category.id, "Orientation"))
      else Validation.unit,
    )
    combineUnit(checks)

  private def resolveComponents(
      category: ProductCategory,
      selections: NonEmptyChunk[ComponentSelection],
      specs: ProductSpecs,
      catalog: ProductCatalog,
  ): Validation[CatalogError, NonEmptyChunk[ConfiguredComponent]] =
    val resolvedListV: Validation[CatalogError, List[ConfiguredComponent]] =
      selections.toList.foldLeft(
        Validation.succeed(List.empty[ConfiguredComponent]): Validation[CatalogError, List[ConfiguredComponent]]
      ) { (accV, sel) =>
        val compV = resolveSingleComponent(category, sel, specs, catalog)
        accV.zipWith(compV)(_ :+ _)
      }
    resolvedListV.flatMap { list =>
      NonEmptyChunk.fromIterableOption(list) match
        case Some(nec) => Validation.succeed(nec)
        case None      => Validation.fail(CatalogError.MissingRequiredComponent(category.id, ComponentKey.Main))
    }

  private def resolveSingleComponent(
      category: ProductCategory,
      sel: ComponentSelection,
      specs: ProductSpecs,
      catalog: ProductCatalog,
  ): Validation[CatalogError, ConfiguredComponent] =
    category.templateFor(sel.key) match
      case None =>
        Validation.fail(CatalogError.UnexpectedComponent(category.id, sel.key))
      case Some(template) =>
        val materialV: Validation[CatalogError, Material] =
          catalog.materials.get(sel.materialId) match
            case None => Validation.fail(CatalogError.MaterialNotFound(sel.materialId))
            case Some(mat) if !mat.enabled =>
              Validation.fail(CatalogError.MaterialDisabled(mat.id))
            case Some(mat) =>
              if template.allowedMaterialIds.contains(mat.id) then Validation.succeed(mat)
              else Validation.fail(CatalogError.MaterialNotAllowed(category.id, sel.key, mat.id))

        val printV: Validation[CatalogError, Option[PrintSpec]] =
          resolvePrintSpec(category, template, sel, catalog)

        val finishesV: Validation[CatalogError, List[SelectedFinish]] =
          resolveFinishes(category, template, sel.finishes, catalog)

        Validation.validateWith(materialV, printV, finishesV)((mat, print, fins) => (mat, print, fins)).flatMap {
          case (mat, print, fins) =>
            GeometryResolver.resolveComponentGeometry(sel.key, mat, template.printable, specs).map { geom =>
              ConfiguredComponent(
                key = sel.key,
                material = mat,
                print = print,
                finishes = fins,
                geometry = geom,
              )
            }
        }

  private def resolvePrintSpec(
      category: ProductCategory,
      template: ComponentTemplate,
      sel: ComponentSelection,
      catalog: ProductCatalog,
  ): Validation[CatalogError, Option[PrintSpec]] =
    if !template.printable then
      if sel.printingMethodId.isDefined || sel.inkConfiguration.isDefined then
        Validation.fail(CatalogError.UnexpectedPrintSpec(sel.key))
      else Validation.succeed(None)
    else
      (sel.printingMethodId, sel.inkConfiguration) match
        case (Some(pmId), Some(ink)) =>
          catalog.printingMethods.get(pmId) match
            case None => Validation.fail(CatalogError.PrintingMethodNotFound(pmId))
            case Some(pm) if !pm.enabled => Validation.fail(CatalogError.PrintingMethodDisabled(pm.id))
            case Some(pm) =>
              val allowedMethods = template.allowedPrintingMethodIds.getOrElse(category.allowedPrintingMethodIds)
              val methodAllowedV =
                if allowedMethods.isEmpty || allowedMethods.contains(pm.id) then Validation.unit
                else Validation.fail(CatalogError.PrintingMethodNotAllowed(category.id, sel.key, pm.id))

              val colorLimitV = pm.maxColorsPerSide match
                case Some(max) if ink.maxChannelsPerSide > max =>
                  Validation.fail(CatalogError.InkExceedsMethodColorLimit(pm.id, sel.key, ink.maxChannelsPerSide, max))
                case _ => Validation.unit

              val duplexV =
                if ink.isDoubleSided && !pm.supportsDoubleSided then
                  Validation.fail(CatalogError.DoubleSidedNotSupported(pm.id, sel.key))
                else Validation.unit

              val specialInksV = combineUnit(
                ink.allSpecialInks.toList.filterNot(pm.supportedSpecialInks.contains).map { unsupported =>
                  Validation.fail(CatalogError.UnsupportedSpecialInk(pm.id, sel.key, unsupported))
                }
              )

              Validation
                .validateWith(methodAllowedV, colorLimitV, duplexV, specialInksV)((_, _, _, _) =>
                  Some(PrintSpec(pm, ink))
                )
        case _ =>
          Validation.fail(CatalogError.MissingPrintSpec(sel.key))

  private def resolveFinishes(
      category: ProductCategory,
      template: ComponentTemplate,
      selections: List[FinishSelection],
      catalog: ProductCatalog,
  ): Validation[CatalogError, List[SelectedFinish]] =
    selections.foldLeft(
      Validation.succeed(List.empty[SelectedFinish]): Validation[CatalogError, List[SelectedFinish]]
    ) { (accV, fs) =>
      val finV = catalog.finishes.get(fs.finishId) match
        case None => Validation.fail(CatalogError.FinishNotFound(fs.finishId))
        case Some(finish) if !finish.enabled => Validation.fail(CatalogError.FinishDisabled(finish.id))
        case Some(finish) =>
          val allowedV =
            if template.allowedFinishIds.contains(finish.id) then Validation.unit
            else Validation.fail(CatalogError.FinishNotAllowed(category.id, template.key, finish.id))

          val sideV   = resolveFinishSide(finish, fs.requestedSide)
          val paramsV = validateFinishParams(finish, fs.params)

          Validation.validateWith(allowedV, sideV, paramsV)((_, side, params) =>
            SelectedFinish(finish, side, params)
          )
      accV.zipWith(finV)(_ :+ _)
    }

  private def resolveFinishSide(
      finish: Finish,
      requested: Option[FinishSide],
  ): Validation[CatalogError, Option[FinishSide]] =
    finish.sidePolicy match
      case FinishSidePolicy.NotApplicable =>
        requested match
          case None    => Validation.succeed(None)
          case Some(s) => Validation.fail(CatalogError.InvalidFinishSide(finish.id, s"Finish does not accept a side ($s)"))
      case FinishSidePolicy.Fixed(fixedSide) =>
        requested match
          case None                     => Validation.succeed(Some(fixedSide))
          case Some(s) if s == fixedSide => Validation.succeed(Some(fixedSide))
          case Some(s) =>
            Validation.fail(CatalogError.InvalidFinishSide(finish.id, s"Side is fixed to $fixedSide, got $s"))
      case FinishSidePolicy.Selectable(allowed, default) =>
        val chosen = requested.getOrElse(default)
        if allowed.contains(chosen) then Validation.succeed(Some(chosen))
        else Validation.fail(CatalogError.InvalidFinishSide(finish.id, s"Side $chosen not in allowed set $allowed"))

  private def validateFinishParams(
      finish: Finish,
      paramsOpt: Option[FinishParams],
  ): Validation[CatalogError, Option[FinishParams]] =
    (finish.paramSchema, paramsOpt) match
      case (FinishParamSchema.NoParams, None) =>
        Validation.succeed(None)
      case (FinishParamSchema.NoParams, Some(p)) =>
        Validation.fail(CatalogError.InvalidFinishParameters(finish.id, s"Finish accepts no parameters, got $p"))
      case (schema, None) =>
        Validation.fail(CatalogError.InvalidFinishParameters(finish.id, s"Missing required parameters for schema $schema"))
      case (FinishParamSchema.RoundCornersSchema(allowedCounts, minR, maxR), Some(p @ FinishParams.RoundCorners(cnt, r))) =>
        val cOk = if allowedCounts.contains(cnt) then Validation.unit
                  else Validation.fail(CatalogError.InvalidFinishParameters(finish.id, s"cornerCount $cnt not in $allowedCounts"))
        val rOk = if r >= minR && r <= maxR then Validation.unit
                  else Validation.fail(CatalogError.InvalidFinishParameters(finish.id, s"radiusMm $r not in [$minR, $maxR]"))
        cOk.zipRight(rOk).as(Some(p))
      case (FinishParamSchema.FoilColorSchema(allowedColors), Some(p @ FinishParams.FoilStamping(color))) =>
        if allowedColors.contains(color) then Validation.succeed(Some(p))
        else Validation.fail(CatalogError.InvalidFinishParameters(finish.id, s"Foil color $color not in $allowedColors"))
      case (FinishParamSchema.GrommetSpacingSchema(allowedSpacings), Some(p @ FinishParams.Grommets(sp))) =>
        if allowedSpacings.contains(sp) then Validation.succeed(Some(p))
        else Validation.fail(CatalogError.InvalidFinishParameters(finish.id, s"Grommet spacing ${sp}mm not in $allowedSpacings"))
      case (FinishParamSchema.PerforationSchema(minP, maxP), Some(p @ FinishParams.Perforation(pitch))) =>
        if pitch >= minP && pitch <= maxP then Validation.succeed(Some(p))
        else Validation.fail(CatalogError.InvalidFinishParameters(finish.id, s"Perforation pitch ${pitch}mm not in [$minP, $maxP]"))
      case (FinishParamSchema.LinearLengthSchema(minM, maxM), Some(p @ FinishParams.LinearLength(len))) =>
        if len >= minM && len <= maxM then Validation.succeed(Some(p))
        else Validation.fail(CatalogError.InvalidFinishParameters(finish.id, s"Length ${len}m not in [$minM, $maxM]"))
      case (FinishParamSchema.CreaseCountSchema(minC, maxC), Some(p @ FinishParams.Scoring(cnt))) =>
        if cnt >= minC && cnt <= maxC then Validation.succeed(Some(p))
        else Validation.fail(CatalogError.InvalidFinishParameters(finish.id, s"Crease count $cnt not in [$minC, $maxC]"))
      case (schema, Some(wrong)) =>
        Validation.fail(CatalogError.InvalidFinishParameters(finish.id, s"Parameter $wrong does not match schema $schema"))

  private def combineUnit(checks: List[Validation[CatalogError, Unit]]): Validation[CatalogError, Unit] =
    checks.foldLeft(Validation.unit: Validation[CatalogError, Unit])(_.zipRight(_))
