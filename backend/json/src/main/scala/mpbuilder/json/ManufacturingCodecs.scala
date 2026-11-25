package mpbuilder.json

import kyo.*
import mpbuilder.json.JsonSupport.*
import mpbuilder.shared.*
import mpbuilder.json.SharedCodecs.given
import mpbuilder.json.JsonSupport.given
import mpbuilder.manufacturing.*

/** Schemas for the parts of the `manufacturing` model surfaced through the BFF facades. */
object ManufacturingCodecs:
  given Schema[WorkflowId] = stringOpaque("WorkflowId")(WorkflowId(_))(_.value)
  given Schema[StepId]     = stringOpaque("StepId")(StepId(_))(_.value)

  given Schema[ManufacturingError] = Schema.derived
