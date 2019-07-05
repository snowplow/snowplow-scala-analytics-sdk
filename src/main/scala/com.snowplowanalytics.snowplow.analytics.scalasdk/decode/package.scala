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

import cats.data.{Validated, ValidatedNel}
import com.snowplowanalytics.snowplow.analytics.scalasdk.ParsingError.RowDecodingErrorInfo
import io.circe.{Decoder, Encoder}
import io.circe.syntax._

package object decode {
  /** Expected name of the field */
  type Key = Symbol

  object Key {
    implicit val analyticsSdkKeyCirceEncoder: Encoder[Key] =
      Encoder.instance(_.toString.stripPrefix("'").asJson)

    implicit val analyticsSdkKeyCirceDecoder: Decoder[Key] =
      Decoder.instance(_.as[String].map(Symbol(_)))
  }

  /** Result of single-value parsing */
  type DecodedValue[A] = Either[RowDecodingErrorInfo, A]

  /** Result of row decode process */
  type RowDecodeResult[A] = ValidatedNel[RowDecodingErrorInfo, A]

  /** Result of TSV line parsing, which is either an event or parse error */
  type DecodeResult[A] = Validated[ParsingError, A]
}
