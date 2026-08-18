package mpbuilder.ui.basket

import mpbuilder.catalog as cat
import mpbuilder.commons.*
import mpbuilder.domain.model as dm
import mpbuilder.domain.codec.ConfigurationCodecs.given
import mpbuilder.domain.sample.{SampleCatalog, SampleRules}
import mpbuilder.domain.service.{ConfigurationBuilder, ConfigurationRequest}
import zio.json.*
import zio.test.*

/** The two translations the server-backed basket rests on.
  *
  * Going out, a resolved configuration is reduced to the request that describes it; coming back, a
  * stored configuration is read out of a basket line. Neither has a caller that would notice a
  * quiet mistranslation: a dropped finish parameter or a lost specification would simply produce a
  * slightly different product at a slightly different price, and look entirely normal.
  */
object ConfigurationRequestsSpec extends ZIOSpecDefault:

  private def resolve(
      components: List[dm.ComponentRequest],
      specs: List[dm.SpecValue],
      categoryId: dm.CategoryId = SampleCatalog.businessCardsId,
      printingMethodId: dm.PrintingMethodId = SampleCatalog.digitalId,
  ): dm.ProductConfiguration =
    ConfigurationBuilder
      .build(
        ConfigurationRequest(categoryId, printingMethodId, components, specs),
        SampleCatalog.catalog,
        SampleRules.ruleset,
        dm.ConfigurationId.unsafe("fixture"),
      )
      .toEither
      .fold(errors => throw new AssertionError(s"fixture did not build: $errors"), identity)

  private val plainCard = resolve(
    components = List(
      dm.ComponentRequest(
        dm.ComponentRole.Main,
        SampleCatalog.coated300gsmId,
        dm.InkConfiguration.cmyk4_4,
        Nil,
      )
    ),
    specs = List(
      dm.SpecValue.SizeSpec(Dimension(90, 55)),
      dm.SpecValue.QuantitySpec(Quantity.unsafe(500)),
    ),
  )

  private val finishedCard = resolve(
    components = List(
      dm.ComponentRequest(
        dm.ComponentRole.Main,
        SampleCatalog.coated300gsmId,
        dm.InkConfiguration.cmyk4_4,
        List(
          dm.FinishSelection(
            SampleCatalog.roundCornersId,
            Some(dm.FinishParameters.RoundCornersParams(4, 3)),
          ),
          dm.FinishSelection(
            SampleCatalog.matteLaminationId,
            Some(dm.FinishParameters.LaminationParams(dm.FinishSide.Both)),
          ),
        ),
      )
    ),
    specs = List(
      dm.SpecValue.SizeSpec(Dimension(90, 55)),
      dm.SpecValue.QuantitySpec(Quantity.unsafe(250)),
      dm.SpecValue.ManufacturingSpeedSpec(dm.ManufacturingSpeed.Express),
    ),
  )

  def spec = suite("ConfigurationRequests")(
    test("names the category, method and material by id") {
      val request = ConfigurationRequests.of(plainCard)
      assertTrue(
        request.categoryId == SampleCatalog.businessCardsId.value,
        request.printingMethodId == SampleCatalog.digitalId.value,
        request.components.map(_.materialId) == List(SampleCatalog.coated300gsmId.value),
        request.components.map(_.role) == List("Main"),
      )
    },
    test("carries the ink setup of both sides") {
      val ink = ConfigurationRequests.of(plainCard).components.head.ink
      assertTrue(
        ink.front.inkType == "CMYK",
        ink.front.colorCount == 4,
        ink.back.inkType == "CMYK",
        ink.back.colorCount == 4,
      )
    },
    test("carries every finish with its parameters") {
      // Dropping a parameter here would order square corners on a product the customer rounded,
      // and the price would move with it.
      val finishes = ConfigurationRequests.of(finishedCard).components.head.finishes
      assertTrue(
        finishes.size == 2,
        finishes.exists(_.params.contains(cat.FinishParamsDto.RoundCorners(4, 3))),
        finishes.exists(_.params.contains(cat.FinishParamsDto.Lamination("Both"))),
      )
    },
    test("carries every specification the configuration holds") {
      val specs = ConfigurationRequests.of(finishedCard).specifications
      assertTrue(
        specs.size.contains(cat.SizeDto(90, 55)),
        specs.quantity.contains(250),
        specs.speed.contains("Express"),
      )
    },
    test("a resolved configuration survives the round trip through the catalog") {
      // The proof that matters: reduce to a request, resolve it exactly as the server's catalog
      // does, and get the same product back. Anything this mapping forgets shows up here as a
      // difference rather than as a mysteriously cheaper order.
      val request = ConfigurationRequests.of(finishedCard)
      val rebuilt = resolve(
        components = request.components.map { c =>
          dm.ComponentRequest(
            role = dm.ComponentRole.valueOf(c.role),
            materialId = dm.MaterialId.unsafe(c.materialId),
            inkConfiguration = dm.InkConfiguration(
              dm.InkSetup(dm.InkType.valueOf(c.ink.front.inkType), c.ink.front.colorCount),
              dm.InkSetup(dm.InkType.valueOf(c.ink.back.inkType), c.ink.back.colorCount),
            ),
            finishes = c.finishes.map(f =>
              dm.FinishSelection(
                dm.FinishId.unsafe(f.finishId),
                f.params.map {
                  case cat.FinishParamsDto.RoundCorners(count, radius) =>
                    dm.FinishParameters.RoundCornersParams(count, radius)
                  case cat.FinishParamsDto.Lamination(side) =>
                    dm.FinishParameters.LaminationParams(dm.FinishSide.valueOf(side))
                  case other => throw new AssertionError(s"unexpected params: $other")
                },
              )
            ),
          )
        },
        specs = List(
          dm.SpecValue.SizeSpec(Dimension(request.specifications.size.get.widthMm, request.specifications.size.get.heightMm)),
          dm.SpecValue.QuantitySpec(Quantity.unsafe(request.specifications.quantity.get)),
          dm.SpecValue.ManufacturingSpeedSpec(
            dm.ManufacturingSpeed.valueOf(request.specifications.speed.get)
          ),
        ),
      )
      assertTrue(
        rebuilt.category.id == finishedCard.category.id,
        rebuilt.printingMethod.id == finishedCard.printingMethod.id,
        rebuilt.components == finishedCard.components,
        rebuilt.specifications == finishedCard.specifications,
      )
    },
    test("a stored configuration is read back exactly as it was stored") {
      // How a reloaded page rebuilds a basket it never configured.
      // Encoded the way the catalog stores it: the snapshot payload is the configuration's JSON.
      val stored = finishedCard.toJson
      assertTrue(ConfigurationPayloads.decode(stored) == Right(finishedCard))
    },
    test("an unreadable payload is an error, not an empty product") {
      assertTrue(ConfigurationPayloads.decode("{not json}").isLeft)
    },
  )
