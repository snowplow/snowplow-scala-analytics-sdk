/*
 * Copyright (c) 2012-2016 Snowplow Analytics Ltd. All rights reserved.
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
package json

// Specs2
import org.specs2.Specification
import org.specs2.ScalaCheck

// ScalaCheck
import org.scalacheck.Arbitrary

/**
 * Tests SnowplowElasticsearchTransformer
 */
class SyntaxSpec extends Specification with ScalaCheck { def is = s2"""
  .traverseEitherL should never throw exceptions $e1
  .traverseEitherL should always preserve errors messages from original list without empty Lefts $e2
  """

  def e1 = {
    prop((s: List[Either[List[String], Int]]) => s.traverseEitherL must not(throwA[Throwable]))
  }

  def e2 = {
    prop((original: List[Either[List[String], Int]]) => {
      val result = original.traverseEitherL
      if (original.count(_.isLeft) == 0) result must beRight else result match {
        case Right(_) => ko
        case Left(errors) => errors.length must beEqualTo(SyntaxSpec.countErrors(original))
      }
    }).setGen(SyntaxSpec.eitherNel)
  }
}

object SyntaxSpec {
  def eitherNel = Arbitrary.arbitrary[List[Either[List[String], Int]]].map { list =>
    list.filter {
      case Left(Nil) => false
      case _ => true
    }
  }

  def countErrors[A, B](eithers: List[Either[List[A], B]]): Int = {
    eithers.collect { case Left(list) => list }.flatten.length
  }
}
