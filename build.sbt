ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.8.1"

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
  .aggregate(domainJVM, domainJS, ui)
  .settings(
    name := "material-builder-root",
    publish / skip := true,
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
      "dev.zio" %%% "zio-test"     % "2.1.16" % Test,
      "dev.zio" %%% "zio-test-sbt" % "2.1.16" % Test,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )

lazy val domainJVM = domain.jvm
lazy val domainJS = domain.js

// UI module (Scala.js only)
lazy val ui = (project in file("modules/ui"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(domainJS)
  .settings(commonSettings)
  .settings(
    name := "material-builder-ui",
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "com.raquo" %%% "laminar" % "17.2.0",
    ),
  )
