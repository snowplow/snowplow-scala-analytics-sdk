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
package com.snowplowanalytics.snowplow.analytics.scalasdk

import cats.data.NonEmptyList
import io.circe.{Decoder, DecodingFailure, Json, parser}
import io.circe.syntax._
import io.circe.parser._
import com.snowplowanalytics.snowplow.analytics.scalasdk.ParsingError._
import com.snowplowanalytics.snowplow.analytics.scalasdk.ParsingError.RowDecodingErrorInfo._
import org.specs2.Specification

import java.time.Instant
import cats.data.Validated.{Invalid, Valid}

import java.util.UUID

class ParsingErrorSpec extends Specification {
  def is = s2"""
  ParsingError encoder-decoder
    works correctly with NotTSV error $e1
    works correctly with FieldNumberMismatch error $e2
    works correctly with RowDecodingError $e3
    works correctly with TSV oversized columns $e4
    works correctly with JSON oversized columns $e5
  """

  def e1 = {
    val errorJson = parseJson(
      """
        |{
        | "type": "NotTSV"
        |}
      """.stripMargin
    )

    val decoded = decodeJson[ParsingError](errorJson)
    val encoded = decoded.asJson

    (decoded must beEqualTo(NotTSV)) and (encoded must beEqualTo(errorJson))
  }

  def e2 = {
    val errorJson = parseJson(
      """
        |{
        | "type": "FieldNumberMismatch",
        | "fieldCount": 120
        |}
      """.stripMargin
    )

    val decoded = decodeJson[ParsingError](errorJson)
    val encoded = decoded.asJson

    (decoded must beEqualTo(FieldNumberMismatch(120))) and (encoded must beEqualTo(errorJson))
  }

  def e3 = {
    val errorJson = parseJson(
      """
        |{
        | "type": "RowDecodingError",
        | "errors": [
        |   {
        |     "type": "InvalidValue",
        |     "key": "exampleKey",
        |     "value": "exampleValue",
        |     "message": "exampleMessage"
        |   },
        |   {
        |     "type": "UnhandledRowDecodingError",
        |     "message": "exampleError"
        |   }
        | ]
        |}
      """.stripMargin
    )

    val decoded = decodeJson[ParsingError](errorJson)
    val encoded = decoded.asJson

    val expected = RowDecodingError(
      NonEmptyList.of(
        InvalidValue(Symbol("exampleKey"), "exampleValue", "exampleMessage"),
        UnhandledRowDecodingError("exampleError")
      )
    )

    (decoded must beEqualTo(expected)) and (encoded must beEqualTo(errorJson))
  }

  def e4 = {
    val badEvent = Event.minimal(UUID.randomUUID(), Instant.now(), "v" * 101, "v_etl").copy(geo_country = Some("sssss"))
    val expected = Invalid(
      RowDecodingError(
        NonEmptyList.of(
          InvalidValue(Symbol("v_collector"), "v" * 101, s"Field v_collector longer than maximum allowed size 100"),
          InvalidValue(Symbol("geo_country"), "sssss", s"Field geo_country longer than maximum allowed size 2")
        )
      )
    )
    (Event.parse(badEvent.toTsv, validateFieldLengths = true) must beEqualTo(expected)) and
      (Event.parse(badEvent.toTsv, validateFieldLengths = false) must haveClass[Valid[_]])
  }

  def e5 =
    // no field length validation since version 3.1.0
    parser.decode[Event](s"""{
        "app_id" :  "bbb05861-0f11-4986-b23b-87e6e22609b1",
        "collector_tstamp" : "2021-12-06T15:47:07.920430Z",
        "event_id" : "bbb05861-0f11-4986-b23b-87e6e22609be",
        "v_collector" : "${"v" * 101}",
        "v_etl" : "v_etl",
        "geo_country" : "sssss",
        "contexts" : {},
        "unstruct_event": {},
        "derived_contexts" : {}
      }""".stripMargin) must beRight

  private def parseJson(jsonStr: String): Json =
    parse(jsonStr).getOrElse(throw new RuntimeException("Failed to parse expected JSON."))

  private def decodeJson[A: Decoder](json: Json): A =
    json.as[A].getOrElse(throw new RuntimeException("Failed to decode to ParsingError."))
}
