package mpbuilder.domain

import zio.test.*
import mpbuilder.domain.model.*
import mpbuilder.domain.service.*
import mpbuilder.domain.sample.*
import com.softwaremill.quicklens.*

object WorkflowGeneratorSpec extends ZIOSpecDefault:

  private val catalog = SampleCatalog.catalog
  private val ruleset = SampleRules.ruleset

  private def buildConfig(
      categoryId: CategoryId,
      printingMethodId: PrintingMethodId,
      components: List[ComponentRequest],
      specs: List[SpecValue],
  ): ProductConfiguration =
    ConfigurationBuilder.build(
      ConfigurationRequest(categoryId, printingMethodId, components, specs),
      catalog,
      ruleset,
      ConfigurationId.unsafe("test-wf"),
    ).toEither.toOption.get

  private def mainComp(
      matId: MaterialId,
      ink: InkConfiguration,
      finishes: List[FinishSelection] = Nil,
  ): ComponentRequest =
    ComponentRequest(ComponentRole.Main, matId, ink, finishes)

  private def coverComp(
      matId: MaterialId,
      ink: InkConfiguration,
      finishes: List[FinishSelection] = Nil,
  ): ComponentRequest =
    ComponentRequest(ComponentRole.Cover, matId, ink, finishes)

  private def bodyComp(
      matId: MaterialId,
      ink: InkConfiguration,
      finishes: List[FinishSelection] = Nil,
  ): ComponentRequest =
    ComponentRequest(ComponentRole.Body, matId, ink, finishes)

  private def generate(config: ProductConfiguration): ManufacturingWorkflow =
    WorkflowGenerator.generate(
      config,
      orderId = OrderId.unsafe("order-1"),
      orderItemIndex = 0,
      workflowId = WorkflowId.unsafe("wf-test"),
    )

  def spec = suite("WorkflowGenerator")(
    suite("basic workflow structure")(
      test("always starts with Prepress and ends with QC + Packaging") {
        val config = buildConfig(
          SampleCatalog.businessCardsId,
          SampleCatalog.digitalId,
          List(mainComp(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4)),
          List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )
        val wf = generate(config)

        assertTrue(
          wf.steps.head.stationType == StationType.Prepress,
          wf.steps.last.stationType == StationType.Packaging,
          wf.steps.init.last.stationType == StationType.QualityControl,
          wf.steps.head.status == StepStatus.Ready,
          wf.steps.head.dependsOn.isEmpty,
        )
      },
      test("prepress step has no dependencies and is Ready") {
        val config = buildConfig(
          SampleCatalog.businessCardsId,
          SampleCatalog.digitalId,
          List(mainComp(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_0)),
          List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )
        val wf = generate(config)

        assertTrue(
          wf.steps.head.dependsOn.isEmpty,
          wf.steps.head.status == StepStatus.Ready,
        )
      },
      test("QC depends on last processing steps") {
        val config = buildConfig(
          SampleCatalog.businessCardsId,
          SampleCatalog.digitalId,
          List(mainComp(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4)),
          List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )
        val wf = generate(config)
        val qcStep = wf.steps.find(_.stationType == StationType.QualityControl).get

        assertTrue(qcStep.dependsOn.nonEmpty)
      },
      test("packaging depends on QC") {
        val config = buildConfig(
          SampleCatalog.businessCardsId,
          SampleCatalog.digitalId,
          List(mainComp(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4)),
          List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )
        val wf = generate(config)
        val packStep = wf.steps.find(_.stationType == StationType.Packaging).get
        val qcStep = wf.steps.find(_.stationType == StationType.QualityControl).get

        assertTrue(packStep.dependsOn == Set(qcStep.id))
      },
    ),
    suite("printing station mapping")(
      test("digital printing method maps to DigitalPrinter station") {
        val config = buildConfig(
          SampleCatalog.businessCardsId,
          SampleCatalog.digitalId,
          List(mainComp(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4)),
          List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )
        val wf = generate(config)

        assertTrue(wf.steps.exists(_.stationType == StationType.DigitalPrinter))
      },
      test("letterpress printing method maps to Letterpress station") {
        val config = buildConfig(
          SampleCatalog.businessCardsId,
          SampleCatalog.letterpressId,
          List(mainComp(SampleCatalog.cottonId, InkConfiguration.mono1_0)),
          List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )
        val wf = generate(config)

        assertTrue(wf.steps.exists(_.stationType == StationType.Letterpress))
      },
      test("UV inkjet maps to LargeFormatPrinter station") {
        val config = buildConfig(
          SampleCatalog.bannersId,
          SampleCatalog.uvInkjetId,
          List(mainComp(SampleCatalog.pvc510gId, InkConfiguration.cmyk4_0)),
          List(
            SpecValue.SizeSpec(Dimension(1000, 500)),
            SpecValue.QuantitySpec(Quantity.unsafe(10)),
          ),
        )
        val wf = generate(config)

        assertTrue(wf.steps.exists(_.stationType == StationType.LargeFormatPrinter))
      },
    ),
    suite("finishing steps")(
      test("lamination finish adds Laminator step") {
        val config = buildConfig(
          SampleCatalog.businessCardsId,
          SampleCatalog.digitalId,
          List(mainComp(
            SampleCatalog.coated300gsmId,
            InkConfiguration.cmyk4_4,
            List(FinishSelection(SampleCatalog.matteLaminationId)),
          )),
          List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )
        val wf = generate(config)

        assertTrue(wf.steps.exists(_.stationType == StationType.Laminator))
      },
      test("UV coating finish adds UVCoater step") {
        val config = buildConfig(
          SampleCatalog.businessCardsId,
          SampleCatalog.digitalId,
          List(mainComp(
            SampleCatalog.coated300gsmId,
            InkConfiguration.cmyk4_4,
            List(FinishSelection(SampleCatalog.uvCoatingId)),
          )),
          List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )
        val wf = generate(config)

        assertTrue(wf.steps.exists(_.stationType == StationType.UVCoater))
      },
      test("embossing finish adds EmbossingFoil step") {
        val config = buildConfig(
          SampleCatalog.businessCardsId,
          SampleCatalog.digitalId,
          List(mainComp(
            SampleCatalog.coated300gsmId,
            InkConfiguration.cmyk4_4,
            List(FinishSelection(SampleCatalog.embossingId)),
          )),
          List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )
        val wf = generate(config)

        assertTrue(wf.steps.exists(_.stationType == StationType.EmbossingFoil))
      },
      test("cutting step is generated for sheet-based products") {
        val config = buildConfig(
          SampleCatalog.businessCardsId,
          SampleCatalog.digitalId,
          List(mainComp(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4)),
          List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )
        val wf = generate(config)

        assertTrue(wf.steps.exists(_.stationType == StationType.Cutter))
      },
    ),
    suite("multi-component products")(
      test("booklet generates steps for both cover and body components") {
        val config = buildConfig(
          SampleCatalog.bookletsId,
          SampleCatalog.digitalId,
          List(
            coverComp(SampleCatalog.coatedGlossy250gsmId, InkConfiguration.cmyk4_4),
            bodyComp(SampleCatalog.coatedGlossy115gsmId, InkConfiguration.cmyk4_4),
          ),
          List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.PagesSpec(16),
            SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          ),
        )
        val wf = generate(config)

        val coverPrint = wf.steps.filter(s =>
          s.componentRole.contains(ComponentRole.Cover) && s.stationType == StationType.DigitalPrinter
        )
        val bodyPrint = wf.steps.filter(s =>
          s.componentRole.contains(ComponentRole.Body) && s.stationType == StationType.DigitalPrinter
        )

        assertTrue(
          coverPrint.size == 1,
          bodyPrint.size == 1,
        )
      },
      test("booklet with binding generates a Binder step") {
        val config = buildConfig(
          SampleCatalog.bookletsId,
          SampleCatalog.digitalId,
          List(
            coverComp(SampleCatalog.coatedGlossy250gsmId, InkConfiguration.cmyk4_4),
            bodyComp(SampleCatalog.coatedGlossy115gsmId, InkConfiguration.cmyk4_4),
          ),
          List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.PagesSpec(16),
            SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          ),
        )
        val wf = generate(config)

        assertTrue(wf.steps.exists(_.stationType == StationType.Binder))
      },
      test("binder step depends on all component steps being done") {
        val config = buildConfig(
          SampleCatalog.bookletsId,
          SampleCatalog.digitalId,
          List(
            coverComp(SampleCatalog.coatedGlossy250gsmId, InkConfiguration.cmyk4_4),
            bodyComp(SampleCatalog.coatedGlossy115gsmId, InkConfiguration.cmyk4_4),
          ),
          List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.PagesSpec(16),
            SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          ),
        )
        val wf = generate(config)
        val binderStep = wf.steps.find(_.stationType == StationType.Binder).get

        assertTrue(binderStep.dependsOn.size >= 2)
      },
    ),
    suite("workflow state management")(
      test("evaluateReadiness promotes Waiting to Ready when dependencies are met") {
        val config = buildConfig(
          SampleCatalog.businessCardsId,
          SampleCatalog.digitalId,
          List(mainComp(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4)),
          List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )
        import ManufacturingWorkflow.*
        val wf = generate(config)

        // Complete the prepress step
        val prepressId = wf.steps.head.id
        val updated = wf
          .modify(_.steps.eachWhere(_.id == prepressId).status).setTo(StepStatus.Completed)
          .evaluateReadiness

        val printStep = updated.steps.find(_.stationType == StationType.DigitalPrinter).get
        assertTrue(printStep.status == StepStatus.Ready)
      },
      test("completionRatio reflects completed steps") {
        val config = buildConfig(
          SampleCatalog.businessCardsId,
          SampleCatalog.digitalId,
          List(mainComp(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_0)),
          List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )
        import ManufacturingWorkflow.*
        val wf = generate(config)

        assertTrue(wf.completionRatio == 0.0)

        val withOneComplete = wf.copy(
          steps = wf.steps.zipWithIndex.map { case (s, i) =>
            if i == 0 then s.copy(status = StepStatus.Completed) else s
          }
        )

        assertTrue(withOneComplete.completionRatio > 0.0)
        assertTrue(withOneComplete.completionRatio < 1.0)
      },
    ),
    suite("folding step")(
      test("brochure with fold type generates Folder step") {
        val config = buildConfig(
          SampleCatalog.brochuresId,
          SampleCatalog.digitalId,
          List(mainComp(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4)),
          List(
            SpecValue.SizeSpec(Dimension(297, 210)),
            SpecValue.QuantitySpec(Quantity.unsafe(200)),
            SpecValue.FoldTypeSpec(FoldType.Tri),
          ),
        )
        val wf = generate(config)

        assertTrue(wf.steps.exists(_.stationType == StationType.Folder))
      },
    ),
    suite("step ordering and dependencies form a valid DAG")(
      test("no step depends on itself") {
        val config = buildConfig(
          SampleCatalog.bookletsId,
          SampleCatalog.digitalId,
          List(
            coverComp(SampleCatalog.coatedGlossy250gsmId, InkConfiguration.cmyk4_4,
              List(FinishSelection(SampleCatalog.matteLaminationId))),
            bodyComp(SampleCatalog.coatedGlossy115gsmId, InkConfiguration.cmyk4_4),
          ),
          List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.PagesSpec(16),
            SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          ),
        )
        val wf = generate(config)

        assertTrue(wf.steps.forall(s => !s.dependsOn.contains(s.id)))
      },
      test("all dependency IDs reference existing steps") {
        val config = buildConfig(
          SampleCatalog.bookletsId,
          SampleCatalog.digitalId,
          List(
            coverComp(SampleCatalog.coatedGlossy250gsmId, InkConfiguration.cmyk4_4,
              List(FinishSelection(SampleCatalog.matteLaminationId))),
            bodyComp(SampleCatalog.coatedGlossy115gsmId, InkConfiguration.cmyk4_4),
          ),
          List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.PagesSpec(16),
            SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          ),
        )
        val wf = generate(config)
        val allIds = wf.steps.map(_.id).toSet

        assertTrue(wf.steps.forall(_.dependsOn.subsetOf(allIds)))
      },
    ),
    suite("external partner routing")(
      test("oversized banner routes to ExternalPartner step; LargeFormatPrinter absent") {
        val config = buildConfig(
          SampleCatalog.bannersId,
          SampleCatalog.uvInkjetId,
          List(mainComp(SampleCatalog.pvc510gId, InkConfiguration.cmyk4_0)),
          List(
            SpecValue.SizeSpec(Dimension(2000, 1000)),
            SpecValue.QuantitySpec(Quantity.unsafe(1)),
          ),
        )
        val externalRules = mpbuilder.domain.validation.RuleEvaluator.collectExternalRules(
          SampleRules.ruleset.rules,
          config.components,
          config.specifications,
          config.category.id,
          config.printingMethod,
        )
        val wf = WorkflowGenerator.generate(
          config,
          orderId = OrderId.unsafe("order-ext-1"),
          orderItemIndex = 0,
          workflowId = WorkflowId.unsafe("wf-ext-1"),
          externalPartners = SamplePartners.allPartners,
          matchingExternalRules = externalRules,
        )
        val stationTypes = wf.steps.map(_.stationType).toSet
        assertTrue(
          stationTypes.contains(StationType.ExternalPartner),
          !stationTypes.contains(StationType.LargeFormatPrinter),
          !stationTypes.contains(StationType.LargeFormatFinishing),
          stationTypes.contains(StationType.Prepress),
          stationTypes.contains(StationType.QualityControl),
          stationTypes.contains(StationType.Packaging),
        )
      },
      test("ExternalPartner step has assignedPartner set and depends on Prepress") {
        val config = buildConfig(
          SampleCatalog.bannersId,
          SampleCatalog.uvInkjetId,
          List(mainComp(SampleCatalog.pvc510gId, InkConfiguration.cmyk4_0)),
          List(
            SpecValue.SizeSpec(Dimension(2000, 1000)),
            SpecValue.QuantitySpec(Quantity.unsafe(1)),
          ),
        )
        val externalRules = mpbuilder.domain.validation.RuleEvaluator.collectExternalRules(
          SampleRules.ruleset.rules,
          config.components,
          config.specifications,
          config.category.id,
          config.printingMethod,
        )
        val wf = WorkflowGenerator.generate(
          config,
          orderId = OrderId.unsafe("order-ext-2"),
          orderItemIndex = 0,
          workflowId = WorkflowId.unsafe("wf-ext-2"),
          externalPartners = SamplePartners.allPartners,
          matchingExternalRules = externalRules,
        )
        val prepressId = wf.steps.find(_.stationType == StationType.Prepress).get.id
        val extStep    = wf.steps.find(_.stationType == StationType.ExternalPartner).get
        assertTrue(
          extStep.assignedPartner == Some(SamplePartners.largePrintPartnerId),
          extStep.dependsOn.contains(prepressId),
          extStep.status == StepStatus.Waiting,
        )
      },
      test("in-house banner (≤1500×1500 mm) does NOT get ExternalPartner step") {
        val config = buildConfig(
          SampleCatalog.bannersId,
          SampleCatalog.uvInkjetId,
          List(mainComp(SampleCatalog.pvc510gId, InkConfiguration.cmyk4_0)),
          List(
            SpecValue.SizeSpec(Dimension(1000, 500)),
            SpecValue.QuantitySpec(Quantity.unsafe(1)),
          ),
        )
        val externalRules = mpbuilder.domain.validation.RuleEvaluator.collectExternalRules(
          SampleRules.ruleset.rules,
          config.components,
          config.specifications,
          config.category.id,
          config.printingMethod,
        )
        val wf = WorkflowGenerator.generate(
          config,
          orderId = OrderId.unsafe("order-ext-3"),
          orderItemIndex = 0,
          workflowId = WorkflowId.unsafe("wf-ext-3"),
          externalPartners = SamplePartners.allPartners,
          matchingExternalRules = externalRules,
        )
        assertTrue(!wf.steps.exists(_.stationType == StationType.ExternalPartner))
      },
    ),
  )
