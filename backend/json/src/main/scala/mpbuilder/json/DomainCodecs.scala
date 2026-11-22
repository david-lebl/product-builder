package mpbuilder.json

/** Single import point for every public domain codec: `import mpbuilder.json.DomainCodecs.given`. */
object DomainCodecs:
  export SharedCodecs.given
  export CatalogCodecs.given
  export PricingCodecs.given
  export CustomerCodecs.given
  export IdentityCodecs.given
  export ArtworkCodecs.given
  export OrderingCodecs.given
  export ManufacturingCodecs.given
