/*
 * Copyright (c) 2016-2018 Snowplow Analytics Ltd. All rights reserved.
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
import sbt._

object Dependencies {

  object V {
    val aws            = "1.11.118"
    val json4s         = "3.2.11"
    // Scala (test only)
    val specs2         = "3.8.9"
    val scalaCheck     = "1.13.4"
  }

  val json4sJackson    = "org.json4s"                 %% "json4s-jackson"       % V.json4s
  val s3               = "com.amazonaws"              % "aws-java-sdk-s3"       % V.aws
  val dynamodb         = "com.amazonaws"              % "aws-java-sdk-dynamodb" % V.aws
  // Scala (test only)
  val specs2           = "org.specs2"                 %% "specs2-core"          % V.specs2     % "test"
  val specs2Scalacheck = "org.specs2"                 %% "specs2-scalacheck"    % V.specs2     % "test"
  val scalaCheck       = "org.scalacheck"             %% "scalacheck"           % V.scalaCheck % "test"
}
