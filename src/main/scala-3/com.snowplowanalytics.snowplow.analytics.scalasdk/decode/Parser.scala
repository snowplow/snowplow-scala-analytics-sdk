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
import cats.data.{NonEmptyList, Validated}
import java.nio.ByteBuffer
import scala.collection.mutable.ListBuffer
import com.snowplowanalytics.snowplow.analytics.scalasdk.ParsingError.{FieldNumberMismatch, NotTSV, RowDecodingError}
import scala.deriving._
import scala.compiletime._

private[scalasdk] trait Parser[A] extends TSVParser[A] {

  /** List of field names defined on `A` */
  def expectedNumFields: Int

  protected def decoder: RowDecoder[A]

  def parse(row: String): DecodeResult[A] = {
    val values = row.split("\t", -1)
    if (values.length == 1) Validated.Invalid(NotTSV)
    else if (values.length != expectedNumFields) Validated.Invalid(FieldNumberMismatch(values.length))
    else decoder(values.toList).leftMap(e => RowDecodingError(e))
  }

  def parseBytes(row: ByteBuffer): DecodeResult[A] = {
    val values = Parser.splitBuffer(row)
    if (values.length == 1) Validated.Invalid(NotTSV)
    else if (values.length != expectedNumFields) Validated.Invalid(FieldNumberMismatch(values.length))
    else decoder.decodeBytes(values.result()).leftMap(e => RowDecodingError(e))
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
    inline def knownKeys(implicit mirror: Mirror.ProductOf[A]): List[String] =
      constValueTuple[mirror.MirroredElemLabels].toArray.map(_.toString).toList

    inline def get(maxLengths: Map[String, Int])(implicit mirror: Mirror.ProductOf[A]): TSVParser[A] =
      new Parser[A] {
        val knownKeys: List[Symbol] = constValueTuple[mirror.MirroredElemLabels].toArray.map(s => Symbol(s.toString)).toList
        val expectedNumFields = knownKeys.length
        val decoder: RowDecoder[A] = RowDecoder.DeriveRowDecoder.of[A].get(knownKeys, maxLengths)
      }
  }

  /** Derive a TSV parser for `A` */
  private[scalasdk] def deriveFor[A]: DeriveParser[A] =
    new DeriveParser[A] {}
}
