package mpbuilder.manufacturing
package impl
package legacy

import mpbuilder.commons.*
import mpbuilder.domain.codec.ConfigurationCodecs.given
import mpbuilder.domain.model as dm
import mpbuilder.domain.service.WorkflowGenerator
import zio.*
import zio.json.*

/** [[WorkflowPort]] backed by the legacy `WorkflowGenerator`.
  *
  * Reading the spec is this adapter's business and nobody else's: the core hands over an opaque
  * string, exactly as order-intake does. When the manufacturing model is extracted in Phase 10,
  * this class is what gets replaced.
  */
private[manufacturing] final class LegacyWorkflowAdapter extends WorkflowPort:

  def plan(
      spec: String,
      lineNo: Int,
      orderId: String,
      quantity: Int,
  ): IO[ProductionError, List[ProductionOrder.Step]] =
    ZIO
      .fromEither(spec.fromJson[dm.ProductConfiguration])
      .mapError(detail =>
        ProductionError.CannotPlan(
          NonEmptyChunk(
            Problem(
              "UnreadableSpec",
              s"The product specification could not be read: $detail",
              s"Specifikaci produktu nelze přečíst: $detail",
            )
          )
        )
      )
      .map { config =>
        val workflow = WorkflowGenerator.generate(
          config = config,
          orderId = dm.OrderId.unsafe(orderId),
          // The generator still identifies its line positionally. The line's real identity is the
          // id this context keeps alongside; this is only what the legacy generator wants.
          orderItemIndex = lineNo - 1,
          workflowId = dm.WorkflowId.unsafe(s"wf-$orderId-$lineNo"),
        )
        workflow.steps.map(step =>
          ProductionOrder.Step(
            id = step.id.value,
            station = step.stationType.toString,
            componentRole = step.componentRole.map(_.toString),
            dependsOn = step.dependsOn.map(_.value),
          )
        )
      }
