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

import io.circe.{Decoder, Json}
import io.circe.syntax._
import io.circe.parser._

import com.snowplowanalytics.snowplow.analytics.scalasdk.ParsingError._
import com.snowplowanalytics.snowplow.analytics.scalasdk.ParsingError.RowDecodingErrorInfo._
import org.specs2.Specification

class ParsingErrorSpec extends Specification { def is = s2"""
  ParsingError encoder-decoder
    works correctly with NonTSVPayload error $e1
    works correctly with ColumnNumberMismatch error $e2
    works correctly with RowDecodingError $e3
  """

  def e1 = {
    val errorJson = parseJson(
      """
        |{
        | "type": "NonTSVPayload"
        |}
      """.stripMargin
    )

    val decoded = decodeJson[ParsingError](errorJson)
    val encoded = decoded.asJson

    (decoded must beEqualTo(NonTSVPayload)) and (encoded must beEqualTo(errorJson))
  }

  def e2 = {
    val errorJson = parseJson(
      """
        |{
        | "type": "ColumnNumberMismatch",
        | "columnCount": 120
        |}
      """.stripMargin
    )

    val decoded = decodeJson[ParsingError](errorJson)
    val encoded = decoded.asJson

    (decoded must beEqualTo(ColumnNumberMismatch(120))) and (encoded must beEqualTo(errorJson))
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
        |     "type": "UnexpectedRowDecodingError",
        |     "error": "exampleError"
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
        UnexpectedRowDecodingError("exampleError")
      )
    )

    (decoded must beEqualTo(expected)) and (encoded must beEqualTo(errorJson))
  }

  private def parseJson(jsonStr: String): Json =
    parse(jsonStr).right.getOrElse(throw new RuntimeException("Failed to parse expected JSON"))

  private def decodeJson[A: Decoder](json: Json): A = {
    json.as[A].right.getOrElse(throw new RuntimeException("Failed to decode to ParsingError"))
  }
}
