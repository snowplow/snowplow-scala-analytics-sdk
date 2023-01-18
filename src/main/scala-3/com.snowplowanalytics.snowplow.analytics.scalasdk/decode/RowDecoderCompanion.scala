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

import cats.syntax.validated._
import cats.syntax.either._
import cats.syntax.apply._
import com.snowplowanalytics.snowplow.analytics.scalasdk.ParsingError.RowDecodingErrorInfo.UnhandledRowDecodingError
import scala.deriving._
import scala.compiletime._

private[scalasdk] trait RowDecoderCompanion {
  def tupled[L <: Tuple](implicit fromRow: RowDecoder[L]): RowDecoder[L] = fromRow

  def fromFunc[L <: Tuple](f: List[(Key, String)] => RowDecodeResult[L]): RowDecoder[L] =
    new RowDecoder[L] {
      def apply(row: List[(Key, String)]) = f(row)
    }

  /** Parse TSV row into HList */
  private def parse[H: ValueDecoder, T <: Tuple: RowDecoder](row: List[(Key, String)]): RowDecodeResult[H *: T] =
    row match {
      case h :: t =>
        val hv: RowDecodeResult[H] = ValueDecoder[H].parse(h).toValidatedNel
        val tv: RowDecodeResult[T] = RowDecoder.tupled[T].apply(t)
        (hv, tv).mapN(_ *: _)
      case Nil => UnhandledRowDecodingError("Not enough values, format is invalid").invalidNel
    }

  implicit def hnilFromRow: RowDecoder[EmptyTuple] =
    fromFunc {
      case Nil => EmptyTuple.validNel
      case rows => UnhandledRowDecodingError(s"No more values expected, following provided: ${rows.map(_._2).mkString(", ")}").invalidNel
    }

  implicit def hconsFromRow[H: ValueDecoder, T <: Tuple: RowDecoder]: RowDecoder[H *: T] =
    fromFunc(row => parse(row))

  inline def of[A](implicit m: Mirror.ProductOf[A]) = {
    val instance = summonInline[RowDecoder[m.MirroredElemTypes]]
    instance.map(tuple => m.fromTuple(tuple))
  }

}
