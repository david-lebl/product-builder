ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.8.1"

lazy val root = (project in file("."))
  .settings(
    name := "material-builder",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"          % "2.1.16",
      "dev.zio" %% "zio-prelude"  % "1.0.0-RC39",
      "dev.zio" %% "zio-test"     % "2.1.16" % Test,
      "dev.zio" %% "zio-test-sbt" % "2.1.16" % Test,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )
