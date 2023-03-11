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

  sealed trait DeriveRowDecoder[L] { self =>
    def get(knownKeys: List[Key], maxLengths: Map[String, Int]): RowDecoder[L]

    def map[B](f: L => B): DeriveRowDecoder[B] =
      new DeriveRowDecoder[B] {
        def get(knownKeys: List[Key], maxLengths: Map[String, Int]): RowDecoder[B] =
          self.get(knownKeys, maxLengths).map(f)
      }
  }

  object DeriveRowDecoder {
    inline def of[L](implicit m: Mirror.ProductOf[L]): DeriveRowDecoder[L] = {
      val instance = summonInline[DeriveRowDecoder[m.MirroredElemTypes]]
      instance.map(tuple => m.fromTuple(tuple))
    }
  }

  private def parse[H: ValueDecoder, T <: Tuple](
    key: Key,
    tailDecoder: RowDecoder[T],
    maxLength: Option[Int],
    row: List[String]
  ): RowDecodeResult[H *: T] =
    row match {
      case h :: t =>
        val hv: RowDecodeResult[H] = ValueDecoder[H].parse(key, h, maxLength).toValidatedNel
        val tv: RowDecodeResult[T] = tailDecoder.apply(t)
        (hv, tv).mapN(_ *: _)
      case Nil => UnhandledRowDecodingError("Not enough values, format is invalid").invalidNel
    }

  implicit def hnilFromRow: DeriveRowDecoder[EmptyTuple] =
    new DeriveRowDecoder[EmptyTuple] {
      def get(knownKeys: List[Key], maxLengths: Map[String, Int]): RowDecoder[EmptyTuple] =
        new RowDecoder[EmptyTuple] {
          def apply(row: List[String]): RowDecodeResult[EmptyTuple] =
            row match {
              case Nil =>
                EmptyTuple.validNel
              case _ =>
                UnhandledRowDecodingError("Not enough values, format is invalid").invalidNel
            }
        }
    }

  implicit def hconsFromRow[H: ValueDecoder, T <: Tuple: DeriveRowDecoder]: DeriveRowDecoder[H *: T] =
    new DeriveRowDecoder[H *: T] {
      def get(knownKeys: List[Key], maxLengths: Map[String, Int]): RowDecoder[H *: T] =
        knownKeys match {
          case key :: tailKeys =>
            val tailDecoder = summon[DeriveRowDecoder[T]].get(tailKeys, maxLengths)
            val maxLength = maxLengths.get(key.name)
            new RowDecoder[H *: T] {
              def apply(row: List[String]): RowDecodeResult[H *: T] = parse(key, tailDecoder, maxLength, row)
            }
          case Nil =>
            // Shapeless type checking makes this impossible
            throw new IllegalStateException
        }
    }

}
