import sbt._

object Dependencies {

  object Versions {
    val log4cats            = "1.7.0"
    val redis4cats          = "0.13.1"
    val cats                = "2.5.0"
    val catsEffect          = "2.4.1"
    val catsTime            = "0.5.0"
    val discipline          = "1.5.1"
    val disciplineScalaTest = "2.0.0"
    val fs2                 = "2.5.4"
    val http4s              = "0.21.22"
    val circe               = "0.13.0"
    val pureConfig          = "0.14.1"

    val kindProjector       = "0.10.3"
    val logback             = "1.2.3"
    val scalaCheck          = "1.15.3"
    val scalaTest           = "3.2.7"
    val catsScalaCheck      = "0.3.0"
  }

  object Libraries {
    def cats(artifact: String): ModuleID = "org.typelevel" %% artifact % Versions.cats
    def circe(artifact: String): ModuleID = "io.circe"    %% artifact % Versions.circe
    def http4s(artifact: String): ModuleID = "org.http4s" %% artifact % Versions.http4s
    def redis4cats(artifact: String): ModuleID = "dev.profunktor" %% artifact % Versions.redis4cats

    lazy val log4cats            = "org.typelevel"         %% "log4cats-slf4j"             % Versions.log4cats
    lazy val catsEffect          = "org.typelevel"         %% "cats-effect"                % Versions.catsEffect
    lazy val catsTime            = "org.typelevel"         %% "cats-time"                  % Versions.catsTime
    lazy val discipline          = "org.typelevel"         %% "discipline-core"            % Versions.discipline
    lazy val disciplineScalaTest = "org.typelevel"         %% "discipline-scalatest"       % Versions.disciplineScalaTest
    lazy val fs2                 = "co.fs2"                %% "fs2-core"                   % Versions.fs2

    lazy val catsCore            = cats("cats-core")
    lazy val catsLaws            = cats("cats-laws")
    lazy val redis4catsLogs      = redis4cats("redis4cats-log4cats")
    lazy val redis4catsCore      = redis4cats("redis4cats-effects")
    lazy val http4sDsl           = http4s("http4s-dsl")
    lazy val http4sServer        = http4s("http4s-blaze-server")
    lazy val http4sCirce         = http4s("http4s-circe")
    lazy val http4sClient        = http4s("http4s-blaze-client")
    lazy val circeCore           = circe("circe-core")
    lazy val circeGeneric        = circe("circe-generic")
    lazy val circeGenericExt     = circe("circe-generic-extras")
    lazy val circeParser         = circe("circe-parser")
    lazy val pureConfig          = "com.github.pureconfig" %% "pureconfig"                 % Versions.pureConfig

    // Compiler plugins
    lazy val kindProjector       = "org.typelevel"         %% "kind-projector"             % Versions.kindProjector

    // Runtime
    lazy val logback             = "ch.qos.logback"        %  "logback-classic"            % Versions.logback

    // Test
    lazy val scalaTest           = "org.scalatest"         %% "scalatest"                  % Versions.scalaTest
    lazy val scalaCheck          = "org.scalacheck"        %% "scalacheck"                 % Versions.scalaCheck
    lazy val catsScalaCheck      = "io.chrisdavenport"     %% "cats-scalacheck"            % Versions.catsScalaCheck
  }

}
