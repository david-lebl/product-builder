ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.3"

// Shared settings for all modules
lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-encoding", "utf-8",
    "-deprecation",
    "-feature",
  )
)

// Root project (JVM-only, aggregates other projects)
lazy val root = (project in file("."))
  .aggregate(domainJVM, domainJS, uiFramework, uiShowcase, ui)
  .settings(
    name := "material-builder-root",
    publish / skip := true,
  )

// UI framework — reusable Laminar components (no domain dependency)
lazy val uiFramework = (project in file("modules/ui-framework"))
  .enablePlugins(ScalaJSPlugin)
  .settings(commonSettings)
  .settings(
    name := "material-builder-ui-framework",
    libraryDependencies += "com.raquo" %%% "laminar" % "17.2.0",
  )

// Domain model cross-compiled for JVM and JS
lazy val domain = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/domain"))
  .settings(commonSettings)
  .settings(
    name := "material-builder-domain",
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio"          % "2.1.16",
      "dev.zio" %%% "zio-prelude"  % "1.0.0-RC39",
      "dev.zio" %%% "zio-json"     % "0.7.3",
      "dev.zio" %%% "zio-test"     % "2.1.16" % Test,
      "dev.zio" %%% "zio-test-sbt" % "2.1.16" % Test,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )

lazy val domainJVM = domain.jvm
lazy val domainJS = domain.js

// UI Kit Showcase — standalone demo app (no domain dependency)
lazy val uiShowcase = (project in file("modules/ui-showcase"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(uiFramework)
  .settings(commonSettings)
  .settings(
    name := "material-builder-ui-showcase",
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "com.raquo" %%% "laminar" % "17.2.0",
    ),
  )

// UI module (Scala.js only)
lazy val ui = (project in file("modules/ui"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(domainJS, uiFramework)
  .settings(commonSettings)
  .settings(
    name := "material-builder-ui",
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "com.raquo" %%% "laminar" % "17.2.0",
    ),
  )
