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
import scala.annotation.tailrec
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
    val numFields = values.length
    if (numFields == 1)
      Validated.Invalid(NotTSV)
    else if (numFields != expectedNumFields)
      Validated.Invalid(FieldNumberMismatch(numFields))
    else {
      val decoded = decoder.decodeBytes(values).leftMap(e => RowDecodingError(e))
      decoded.map(decodedValue => generic.from(decodedValue))
    }
  }
}

object Parser {

  private val tab: Byte = '\t'.toByte

  /**
   * Splits the row into its TSV-delimited slices.
   *
   * Walks backwards from the end of the row, so that prepending yields the fields in order and
   * neither a builder nor a final reverse is needed.
   *
   * The walk must keep its index a primitive `Int`, which is why it is a tail recursion and not
   * `(from until to).foreach`. The latter dispatches through the generic
   * `Range.foreach(Function1)`, which boxes the index into a `java.lang.Integer` on every byte of
   * every event parsed. Allocation here should be proportional to the number of fields, never to
   * the number of bytes.
   */
  private[decode] def splitBuffer(row: ByteBuffer): List[ByteBuffer] = {
    val from = row.position()

    def slice(start: Int, until: Int): ByteBuffer = row.duplicate.position(start).limit(until)

    @tailrec
    def go(
      end: Int,
      i: Int,
      acc: List[ByteBuffer]
    ): List[ByteBuffer] =
      if (i < from) slice(from, end) :: acc
      else if (row.get(i) == tab) go(i, i - 1, slice(i + 1, end) :: acc)
      else go(end, i - 1, acc)

    go(row.limit(), row.limit() - 1, Nil)
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
