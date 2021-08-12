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
import sbt._

object Dependencies {

  object V {
    val igluCore = "1.0.0"
    val cats = "2.1.1"
    val circe = "0.13.0"
    val fs2 = "2.4.4"
    // Scala (test only)
    val specs2 = "4.8.0"
    val scalaCheck = "1.15.0"
    val specs2CE = "0.4.1"
  }

  val igluCore = "com.snowplowanalytics" %% "iglu-core-circe" % V.igluCore
  val cats = "org.typelevel" %% "cats-core" % V.cats
  val circeParser = "io.circe" %% "circe-parser" % V.circe
  val circeGeneric = "io.circe" %% "circe-generic" % V.circe
  val fs2 = "co.fs2" %% "fs2-core" % V.fs2
  val fs2Io = "co.fs2" %% "fs2-io" % V.fs2
  // Scala (test only)
  val specs2 = "org.specs2" %% "specs2-core" % V.specs2 % Test
  val specs2Scalacheck = "org.specs2" %% "specs2-scalacheck" % V.specs2 % Test
  val scalacheck = "org.scalacheck" %% "scalacheck" % V.scalaCheck % Test
  val specs2Cats = "org.specs2" %% "specs2-cats" % V.specs2 % Test
  val specs2CE = "com.codecommit" %% "cats-effect-testing-specs2" % V.specs2CE % Test
}
