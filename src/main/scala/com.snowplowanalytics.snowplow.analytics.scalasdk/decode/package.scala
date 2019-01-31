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

import cats.data.ValidatedNel

package object decode {
  /** Expected name of the field */
  type Key = Symbol

  /** Result of single-value parsing */
  type DecodedValue[A] = Either[(Key, String), A]

  /** Result of TSV line parsing, which is either an event or non empty list of parse errors */
  type RowDecodeResult[A] = ValidatedNel[String, A]
}
