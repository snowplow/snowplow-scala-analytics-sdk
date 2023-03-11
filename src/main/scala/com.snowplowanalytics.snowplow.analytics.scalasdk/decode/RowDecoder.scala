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
package decode

private[scalasdk] trait RowDecoder[L] extends Serializable { self =>
  def apply(row: List[String]): RowDecodeResult[L]
  def map[B](f: L => B): RowDecoder[B] =
    new RowDecoder[B] {
      def apply(row: List[String]): RowDecodeResult[B] = self.apply(row).map(f)
    }
}

object RowDecoder extends RowDecoderCompanion
