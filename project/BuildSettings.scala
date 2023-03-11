/*
 * Copyright (c) 2016-2019 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

// SBT
import sbt._
import Keys._


// Mima plugin
import com.typesafe.tools.mima.plugin.MimaKeys._
import com.typesafe.tools.mima.core.{ProblemFilters, DirectMissingMethodProblem}

// Scoverage plugin
import scoverage.ScoverageKeys._


import sbtdynver.DynVerPlugin.autoImport._

import com.typesafe.sbt.site.SitePlugin.autoImport._
import com.typesafe.sbt.site.SiteScaladocPlugin.autoImport._
import com.typesafe.sbt.site.preprocess.PreprocessPlugin.autoImport._

import org.scalafmt.sbt.ScalafmtPlugin.autoImport._

object BuildSettings {

  // Basic settings for our app
  lazy val buildSettings = Seq(
    scalacOptions      ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-unchecked"
    ),
    scalacOptions ++= {
      if (scalaVersion.value.startsWith("3")) {
        Seq("-Xmax-inlines", "150")
      } else {
        Seq(
          "-Ywarn-dead-code",
          "-Ywarn-numeric-widen",
          "-Ywarn-value-discard"
        )
      }
    }
  )

  lazy val dynVerSettings = Seq(
    ThisBuild / dynverVTagPrefix := false, // Otherwise git tags required to have v-prefix
    ThisBuild / dynverSeparator := "-" // to be compatible with docker
  )

  lazy val publishSettings = Seq[Setting[_]](
    publishArtifact := true,
    Test / publishArtifact := false,
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")),
    pomIncludeRepository := { _ => false },
    homepage := Some(url("http://snowplowanalytics.com")),
    developers := List(
      Developer(
        "Snowplow Analytics Ltd",
        "Snowplow Analytics Ltd",
        "support@snowplowanalytics.com",
        url("https://snowplowanalytics.com")
      )
    )
  )

  // If new version introduces breaking changes, clear out the lists of previous version.
  // Otherwise, add previous version to set without removing other versions.
  val mimaPreviousVersionsScala2 = Set("3.0.1")
  val mimaPreviousVersionsScala3 = Set()
  lazy val mimaSettings = Seq(
    mimaPreviousArtifacts := {
      val versionsForBuild = 
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((3, _)) =>
            mimaPreviousVersionsScala3
          case _ =>
            mimaPreviousVersionsScala2
        }
          
      versionsForBuild.map { organization.value %% name.value % _ }
    },
    ThisBuild / mimaFailOnNoPrevious := false,
    mimaBinaryIssueFilters ++= Seq(
      // DeriveParser should not have been public in previous versions
      ProblemFilters.exclude[DirectMissingMethodProblem]("com.snowplowanalytics.snowplow.analytics.scalasdk.decode.Parser#DeriveParser.get")
    ),
    Test / test := (Test / test).dependsOn(mimaReportBinaryIssues).value
  )

  val scoverageSettings = Seq(
    coverageMinimumStmtTotal := 50,
    // Excluded because of shapeless, which would generate 1000x500KB statements driving coverage OOM
    coverageExcludedFiles := """.*\/Event.*;""",
    coverageFailOnMinimum := true,
    coverageHighlighting := false
  )

  lazy val sbtSiteSettings = Seq(
    SiteScaladoc / siteSubdirName := s"${version.value}",
    Preprocess  / preprocessVars := Map("VERSION" -> version.value)
  )

  lazy val formattingSettings = Seq(
    scalafmtConfig    := file(".scalafmt.conf"),
    scalafmtOnCompile := true
  )

}
