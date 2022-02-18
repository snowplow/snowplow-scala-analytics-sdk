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
    scalacOptions      := Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-unchecked",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard"
    )
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

  // If new version introduces breaking changes,
  // clear-out mimaBinaryIssueFilters and mimaPreviousVersions.
  // Otherwise, add previous version to set without
  // removing other versions.
  val mimaPreviousVersions = Set()

  val mimaSettings = Seq(
    mimaPreviousArtifacts := mimaPreviousVersions.map { organization.value %% name.value % _ },
    ThisBuild / mimaFailOnNoPrevious := false,
    mimaBinaryIssueFilters ++= Seq(),
    Test / test := {
      mimaReportBinaryIssues.value
      (Test / test).value
    }
  )

  val scoverageSettings = Seq(
    coverageMinimum := 50,
    // Excluded because of shapeless, which would generate 1000x500KB statements driving coverage OOM
    coverageExcludedFiles := """.*\/Event.*;""",
    coverageFailOnMinimum := true,
    coverageHighlighting := false,
    (test in Test) := {
      (coverageReport dependsOn (test in Test)).value
    }
  )

  lazy val sbtSiteSettings = Seq(
    siteSubdirName in SiteScaladoc := s"${version.value}",
    preprocessVars in Preprocess := Map("VERSION" -> version.value)
  )

  lazy val formattingSettings = Seq(
    scalafmtConfig    := file(".scalafmt.conf"),
    scalafmtOnCompile := true
  )
}
