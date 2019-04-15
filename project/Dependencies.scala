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
    val igluCore       = "0.5.0-M1"
    val cats           = "1.6.0"
    val circe          = "0.11.1"
    val aws            = "1.11.490"
    // Scala (test only)
    val specs2         = "4.4.1"
  }

  val igluCore         = "com.snowplowanalytics"      %% "iglu-core-circe"      % V.igluCore
  val cats             = "org.typelevel"              %% "cats-core"            % V.cats
  val circeParser      = "io.circe"                   %% "circe-parser"         % V.circe
  val circeGeneric     = "io.circe"                   %% "circe-generic"        % V.circe
  val circeJava        = "io.circe"                   %% "circe-java8"          % V.circe
  val s3               = "com.amazonaws"              % "aws-java-sdk-s3"       % V.aws
  val dynamodb         = "com.amazonaws"              % "aws-java-sdk-dynamodb" % V.aws
  // Scala (test only)
  val specs2           = "org.specs2"                 %% "specs2-core"          % V.specs2     % "test"
}
