package mpbuilder.domain.manufacturing

import mpbuilder.domain.model.LocalizedString

object SampleStations:

  val prepress: Station = Station(
    id = StationId.unsafe("station-prepress"),
    name = LocalizedString("Prepress", "Předtisková příprava"),
    stationType = StationType.Prepress,
    sortOrder = 1,
  )

  val printing: Station = Station(
    id = StationId.unsafe("station-printing"),
    name = LocalizedString("Digital Printing", "Digitální tisk"),
    stationType = StationType.DigitalPrinter,
    sortOrder = 2,
  )

  val cutting: Station = Station(
    id = StationId.unsafe("station-cutting"),
    name = LocalizedString("Cutting", "Řez"),
    stationType = StationType.Cutter,
    sortOrder = 3,
  )

  val lamination: Station = Station(
    id = StationId.unsafe("station-lamination"),
    name = LocalizedString("Lamination", "Laminace"),
    stationType = StationType.Laminator,
    sortOrder = 4,
  )

  val folding: Station = Station(
    id = StationId.unsafe("station-folding"),
    name = LocalizedString("Folding", "Bigování"),
    stationType = StationType.Folder,
    sortOrder = 5,
  )

  val binding: Station = Station(
    id = StationId.unsafe("station-binding"),
    name = LocalizedString("Binding", "Vazba"),
    stationType = StationType.Binder,
    sortOrder = 6,
  )

  val qualityControl: Station = Station(
    id = StationId.unsafe("station-qc"),
    name = LocalizedString("Quality Control", "Kontrola kvality"),
    stationType = StationType.QualityControl,
    sortOrder = 7,
  )

  val packaging: Station = Station(
    id = StationId.unsafe("station-packaging"),
    name = LocalizedString("Packaging", "Balení"),
    stationType = StationType.Packaging,
    sortOrder = 8,
  )

  val allStations: List[Station] = List(
    prepress, printing, cutting, lamination, folding, binding, qualityControl, packaging
  ).sortBy(_.sortOrder)
