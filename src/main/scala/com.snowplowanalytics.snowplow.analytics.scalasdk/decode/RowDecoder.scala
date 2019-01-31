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

/**
  * Type class to decode List of keys-value pairs into HList
  * Keys derived from original class of HList,
  * Values are actual TSV columns
  */
private[scalasdk] trait RowDecoder[L <: HList] extends Serializable {
  def apply(row: List[(Key, String)]): DecodeResult[L]
}

private[scalasdk] object RowDecoder {
  import HList.ListCompat._

  def apply[L <: HList](implicit fromRow: RowDecoder[L]): RowDecoder[L] = fromRow

  def fromFunc[L <: HList](f: List[(Key, String)] => DecodeResult[L]): RowDecoder[L] =
    new RowDecoder[L] {
      def apply(row: List[(Key, String)]) = f(row)
    }

  /** Parse TSV row into HList */
  private def parse[H: ValueDecoder, T <: HList: RowDecoder](row: List[(Key, String)]) =
    row match {
      case h :: t =>
        val hv: DecodeResult[H] =
          ValueDecoder[H].parse(h).leftMap(_._2).toValidatedNel
        val tv = RowDecoder[T].apply(t)
        (hv, tv).mapN { _ :: _ }
      case Nil => "Not enough values, format is invalid".invalidNel
    }

  implicit val hnilFromRow: RowDecoder[HNil] = fromFunc {
    case Nil => HNil.validNel
    case rows => s"No more values expected, following provided: ${rows.map(_._2).mkString(", ")}".invalidNel
  }

  implicit def hconsFromRow[H: ValueDecoder, T <: HList: RowDecoder]: RowDecoder[H :: T] =
    fromFunc { row => parse(row) }
}

