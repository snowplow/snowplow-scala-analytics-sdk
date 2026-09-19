/*
 * Copyright (c) 2016-2026 Snowplow Analytics Ltd. All rights reserved.
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

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import org.specs2.mutable.Specification

class ParserSpec extends Specification {

  "splitBuffer" should {
    "split a row on tabs" in {
      split("a\tbb\tccc") must_== List("a", "bb", "ccc")
    }

    "return a single value for a row with no tabs" in {
      split("abc") must_== List("abc")
    }

    "return a single empty value for an empty row" in {
      split("") must_== List("")
    }

    "preserve leading, trailing and interior empty fields" in {
      split("\ta\t\tb\t") must_== List("", "a", "", "b", "")
    }

    "start from the buffer's position, not from zero" in {
      val buffer = ByteBuffer.wrap("xxxa\tb".getBytes(StandardCharsets.UTF_8))
      buffer.position(3)
      decodeAll(Parser.splitBuffer(buffer)) must_== List("a", "b")
    }
  }

  private def split(row: String): List[String] =
    decodeAll(Parser.splitBuffer(ByteBuffer.wrap(row.getBytes(StandardCharsets.UTF_8))))

  private def decodeAll(buffers: List[ByteBuffer]): List[String] =
    buffers.map(b => StandardCharsets.UTF_8.decode(b).toString)
}
