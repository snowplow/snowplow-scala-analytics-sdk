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
import cats.syntax.validated._
import cats.syntax.either._
import cats.syntax.apply._
import java.nio.ByteBuffer
import com.snowplowanalytics.snowplow.analytics.scalasdk.ParsingError.RowDecodingErrorInfo.UnhandledRowDecodingError

private[scalasdk] trait RowDecoderCompanion {
  import HList.ListCompat._

  sealed trait DeriveRowDecoder[L] {
    def get(knownKeys: List[Key], maxLengths: Map[String, Int]): RowDecoder[L]
  }

  object DeriveRowDecoder {
    def apply[L](implicit fromRow: DeriveRowDecoder[L]): DeriveRowDecoder[L] = fromRow
  }

  /** Parse TSV row into HList */
  private def parse[H: ValueDecoder, T <: HList](
    key: Key,
    tailDecoder: RowDecoder[T],
    maxLength: Option[Int],
    row: List[String]
  ): RowDecodeResult[H :: T] =
    row match {
      case h :: t =>
        val hv: RowDecodeResult[H] = ValueDecoder[H].parse(key, h, maxLength).toValidatedNel
        val tv: RowDecodeResult[T] = tailDecoder.apply(t)
        (hv, tv).mapN(_ :: _)
      case Nil => UnhandledRowDecodingError("Not enough values, format is invalid").invalidNel
    }

  /** Parse TSV row into HList */
  private def parseBytes[H: ValueDecoder, T <: HList](
    key: Key,
    tailDecoder: RowDecoder[T],
    maxLength: Option[Int],
    row: List[ByteBuffer]
  ): RowDecodeResult[H :: T] =
    row match {
      case h :: t =>
        val hv: RowDecodeResult[H] = ValueDecoder[H].parseBytes(key, h, maxLength).toValidatedNel
        val tv: RowDecodeResult[T] = tailDecoder.decodeBytes(t)
        (hv, tv).mapN(_ :: _)
      case Nil => UnhandledRowDecodingError("Not enough values, format is invalid").invalidNel
    }

  implicit def hnilFromRow: DeriveRowDecoder[HNil] =
    new DeriveRowDecoder[HNil] {
      def get(knownKeys: List[Key], maxLengths: Map[String, Int]): RowDecoder[HNil] =
        new RowDecoder[HNil] {
          def apply(row: List[String]): RowDecodeResult[HNil] =
            row match {
              case Nil =>
                HNil.validNel
              case _ =>
                UnhandledRowDecodingError("Not enough values, format is invalid").invalidNel
            }

          def decodeBytes(row: List[ByteBuffer]): RowDecodeResult[HNil] =
            row match {
              case Nil =>
                HNil.validNel
              case _ =>
                UnhandledRowDecodingError("Not enough values, format is invalid").invalidNel
            }
        }
    }

  implicit def hconsFromRow[H: ValueDecoder, T <: HList: DeriveRowDecoder]: DeriveRowDecoder[H :: T] =
    new DeriveRowDecoder[H :: T] {
      def get(knownKeys: List[Key], maxLengths: Map[String, Int]): RowDecoder[H :: T] =
        knownKeys match {
          case key :: tailKeys =>
            val tailDecoder = DeriveRowDecoder.apply[T].get(tailKeys, maxLengths)
            val maxLength = maxLengths.get(key.name)
            new RowDecoder[H :: T] {
              def apply(row: List[String]): RowDecodeResult[H :: T] = parse(key, tailDecoder, maxLength, row)
              def decodeBytes(row: List[ByteBuffer]): RowDecodeResult[H :: T] = parseBytes(key, tailDecoder, maxLength, row)
            }
          case Nil =>
            // Shapeless type checking makes this impossible
            throw new IllegalStateException
        }
    }
}
