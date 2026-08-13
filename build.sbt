import org.scalajs.linker.interface.ModuleSplitStyle
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

val zioV           = "2.1.14"
val zioHttpV       = "3.0.1"
val zioJsonV       = "0.7.3"
val zioPreludeV    = "1.0.0-RC31"
val zioConfigV     = "4.0.2"
val quillV         = "4.8.5"
val pgV            = "42.7.4"
val flywayV        = "10.20.1"
val laminarV       = "17.2.0"
val scalaJavaTimeV = "2.6.0"

ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.8.1"
ThisBuild / organization := "mpbuilder"

lazy val root = (project in file("."))
  .aggregate(shared.jvm, shared.js, backend, frontend)
  .settings(
    name           := "material-builder",
    publish / skip := true,
  )

lazy val shared = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/shared"))
  .settings(
    name := "mpbuilder-shared",
    scalacOptions += "-Xmax-inlines:128",
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio-json"     % zioJsonV,
      "dev.zio" %%% "zio-prelude"  % zioPreludeV,
      "dev.zio" %%% "zio-test"     % zioV % Test,
      "dev.zio" %%% "zio-test-sbt" % zioV % Test,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )
  .jsSettings(
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeV
  )

lazy val backend = project
  .in(file("modules/backend"))
  .dependsOn(shared.jvm)
  .settings(
    name := "mpbuilder-backend",
    libraryDependencies ++= Seq(
      "dev.zio"        %% "zio"                          % zioV,
      "dev.zio"        %% "zio-http"                     % zioHttpV,
      "io.getquill"    %% "quill-jdbc-zio"               % quillV,
      "org.postgresql"  % "postgresql"                   % pgV,
      "org.flywaydb"    % "flyway-database-postgresql"   % flywayV,
      "dev.zio"        %% "zio-config"                   % zioConfigV,
      "dev.zio"        %% "zio-config-typesafe"          % zioConfigV,
      "dev.zio"        %% "zio-test"                     % zioV % Test,
      "dev.zio"        %% "zio-test-sbt"                 % zioV % Test,
      "com.dimafeng"   %% "testcontainers-scala-postgresql" % "0.41.4" % Test,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    run / fork := true,
  )

lazy val frontend = project
  .in(file("modules/frontend"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(shared.js)
  .settings(
    name                            := "mpbuilder-frontend",
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("mpbuilder")))
    },
    libraryDependencies ++= Seq(
      "com.raquo" %%% "laminar" % laminarV
    ),
  )
