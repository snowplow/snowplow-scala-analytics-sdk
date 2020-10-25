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

import shapeless._
import shapeless.ops.record._
import shapeless.ops.hlist._
import cats.data.{NonEmptyList, Validated}
import com.snowplowanalytics.snowplow.analytics.scalasdk.ParsingError.{FieldNumberMismatch, NotTSV, RowDecodingError}

private[scalasdk] trait Parser[A] extends Serializable {

  /** Heterogeneous TSV values */
  type HTSV <: HList

  /** List of field names defined on `A` */
  def knownKeys: List[Key] // TODO: should be a part of `RowDecoder`

  /** Evidence allowing to transform TSV line into `HList` */
  protected def decoder: RowDecoder[HTSV]

  /** Evidence that `A` is isomorphic to `HTSV` */
  protected def generic: Generic.Aux[A, HTSV]

  def parse(row: String): DecodeResult[A] = {
    val values = row.split("\t", -1)
    if (values.length == 1)
      Validated.Invalid(NotTSV)
    else if (values.length != knownKeys.length)
      Validated.Invalid(FieldNumberMismatch(values.length))
    else {
      val zipped = knownKeys.zip(values)
      val decoded = decoder(zipped).leftMap(e => RowDecodingError(e))
      decoded.map(decodedValue => generic.from(decodedValue))
    }
  }
}

object Parser {
  sealed trait DeriveParser[A] {

    /**
     * Get instance of parser after all evidences are given
     * @tparam R full class representation with field names and types
     * @tparam K evidence of field names
     * @tparam L evidence of field types
     */
    def get[R <: HList, K <: HList, L <: HList](
      implicit lgen: LabelledGeneric.Aux[A, R],
      keys: Keys.Aux[R, K],
      gen: Generic.Aux[A, L],
      toTraversableAux: ToTraversable.Aux[K, List, Symbol],
      rowDecoder: RowDecoder[L]
    ): Parser[A] =
      new Parser[A] {
        type HTSV = L
        val knownKeys: List[Symbol] = keys.apply.toList[Symbol]
        val decoder: RowDecoder[L] = rowDecoder
        val generic: Generic.Aux[A, L] = gen
      }
  }

  /** Derive a TSV parser for `A` */
  private[scalasdk] def deriveFor[A]: DeriveParser[A] =
    new DeriveParser[A] {}
}
