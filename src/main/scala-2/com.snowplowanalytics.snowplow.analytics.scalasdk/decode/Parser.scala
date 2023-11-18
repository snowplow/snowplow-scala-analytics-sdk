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

import cats.implicits._
import shapeless._
import shapeless.ops.record._
import shapeless.ops.hlist._
import cats.data.{NonEmptyList, Validated}
import java.nio.ByteBuffer
import scala.collection.mutable.ListBuffer
import com.snowplowanalytics.snowplow.analytics.scalasdk.ParsingError.{FieldNumberMismatch, NotTSV, RowDecodingError}

private[scalasdk] trait Parser[A] extends TSVParser[A] {

  /** Heterogeneous TSV values */
  type HTSV <: HList

  def expectedNumFields: Int

  /** Evidence allowing to transform TSV line into `HList` */
  protected def decoder: RowDecoder[HTSV]

  /** Evidence that `A` is isomorphic to `HTSV` */
  protected def generic: Generic.Aux[A, HTSV]

  def parse(row: String): DecodeResult[A] = {
    val values = row.split("\t", -1)
    if (values.length == 1)
      Validated.Invalid(NotTSV)
    else if (values.length != expectedNumFields)
      Validated.Invalid(FieldNumberMismatch(values.length))
    else {
      val decoded = decoder(values.toList).leftMap(e => RowDecodingError(e))
      decoded.map(decodedValue => generic.from(decodedValue))
    }
  }

  def parseBytes(row: ByteBuffer): DecodeResult[A] = {
    val values = Parser.splitBuffer(row)
    if (values.length == 1)
      Validated.Invalid(NotTSV)
    else if (values.length != expectedNumFields)
      Validated.Invalid(FieldNumberMismatch(values.length))
    else {
      val decoded = decoder.decodeBytes(values.result()).leftMap(e => RowDecodingError(e))
      decoded.map(decodedValue => generic.from(decodedValue))
    }
  }
}

object Parser {

  private val tab: Byte = '\t'.toByte

  private def splitBuffer(row: ByteBuffer): ListBuffer[ByteBuffer] = {
    var current = row.duplicate
    val builder = ListBuffer(current)
    (row.position() until row.limit()).foreach { i =>
      if (row.get(i) === tab) {
        current.limit(i)
        current = row.duplicate.position(i + 1)
        builder += current
      }
    }
    builder
  }

  private[scalasdk] sealed trait DeriveParser[A] {

    def knownKeys[R <: HList, K <: HList, L <: HList](
      implicit lgen: LabelledGeneric.Aux[A, R],
      keys: Keys.Aux[R, K],
      gen: Generic.Aux[A, L],
      toTraversableAux: ToTraversable.Aux[K, List, Symbol]
    ): List[String] =
      keys().toList.map(_.name)

    /**
     * Get instance of parser after all evidences are given
     * @tparam R full class representation with field names and types
     * @tparam K evidence of field names
     * @tparam L evidence of field types
     */
    def get[R <: HList, K <: HList, L <: HList](
      maxLengths: Map[String, Int]
    )(
      implicit lgen: LabelledGeneric.Aux[A, R],
      keys: Keys.Aux[R, K],
      gen: Generic.Aux[A, L],
      toTraversableAux: ToTraversable.Aux[K, List, Symbol],
      deriveRowDecoder: RowDecoder.DeriveRowDecoder[L]
    ): TSVParser[A] =
      new Parser[A] {
        type HTSV = L
        val keyList = keys().toList
        val expectedNumFields: Int = keyList.length
        val decoder: RowDecoder[L] = deriveRowDecoder.get(keyList, maxLengths)
        val generic: Generic.Aux[A, L] = gen
      }
  }

  /** Derive a TSV parser for `A` */
  private[scalasdk] def deriveFor[A]: DeriveParser[A] =
    new DeriveParser[A] {}
}
