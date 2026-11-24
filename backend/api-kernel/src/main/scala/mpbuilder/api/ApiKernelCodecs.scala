package mpbuilder.api

import kyo.*

/** Schemas shared by every BFF contract: all domain schemas plus the kernel's own wire types. */
object ApiKernelCodecs:
  export mpbuilder.json.DomainCodecs.given

  given Schema[HealthStatus]                         = Schema.derived
  given apiErrorSchema[E: Schema]: Schema[ApiError[E]] = Schema.derived
