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
package com.snowplowanalytics.snowplow.analytics.scalasdk.decode

import cats.data.{NonEmptyList, Validated}
import com.snowplowanalytics.snowplow.analytics.scalasdk.ParsingError.{FieldNumberMismatch, NotTSV, RowDecodingError}
import scala.deriving._
import scala.compiletime._

private[scalasdk] trait Parser[A] extends Serializable {

  /** List of field names defined on `A` */
  def knownKeys: List[Key]

  protected def decoder: RowDecoder[A]

  def parse(row: String): DecodeResult[A] = {
    val values = row.split("\t", -1)
    if (values.length == 1) Validated.Invalid(NotTSV)
    else if (values.length != knownKeys.length) Validated.Invalid(FieldNumberMismatch(values.length))
    else decoder(values.toList).leftMap(e => RowDecodingError(e))
  }
}

object Parser {
  sealed trait DeriveParser[A] {
    inline def get(maxLengths: Map[String, Int])(implicit mirror: Mirror.ProductOf[A]): Parser[A] =
      new Parser[A] {
        val knownKeys: List[Symbol] = constValueTuple[mirror.MirroredElemLabels].toArray.map(s => Symbol(s.toString)).toList
        val decoder: RowDecoder[A] = RowDecoder.DeriveRowDecoder.of[A].get(knownKeys, maxLengths)
      }
  }

  /** Derive a TSV parser for `A` */
  private[scalasdk] def deriveFor[A]: DeriveParser[A] =
    new DeriveParser[A] {}
}
