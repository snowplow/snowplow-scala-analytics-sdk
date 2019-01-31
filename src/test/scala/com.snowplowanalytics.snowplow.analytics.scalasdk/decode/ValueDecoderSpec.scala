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

// java
import java.time.Instant
import java.util.UUID

// circe
import io.circe.{Json, JsonObject}
import io.circe.syntax._
import io.circe.parser._

// cats
import cats.syntax.either._

// Specs2
import org.specs2.mutable.Specification

// Iglu
import com.snowplowanalytics.iglu.core.{SchemaKey, SchemaVer, SelfDescribingData}

// This library
import com.snowplowanalytics.snowplow.analytics.scalasdk.SnowplowEvent.{Contexts, UnstructEvent}

/**
  * Tests ValueDecoder class
  */
class ValueDecoderSpec extends Specification {

  "The ValueDecoder class" should {
    "parse String and Option[String] values" in {
      ValueDecoder[String].parse(Symbol("key"), "") mustEqual (Symbol("key"), "Field 'key cannot be empty").asLeft
      ValueDecoder[String].parse(Symbol("key"), "value") mustEqual "value".asRight
      ValueDecoder[Option[String]].parse(Symbol("key"), "") mustEqual None.asRight
      ValueDecoder[Option[String]].parse(Symbol("key"), "value") mustEqual Some("value").asRight
    }

    "parse Option[Int] values" in {
      ValueDecoder[Option[Int]].parse(Symbol("key"), "") mustEqual None.asRight
      ValueDecoder[Option[Int]].parse(Symbol("key"), "42") mustEqual Some(42).asRight
      ValueDecoder[Option[Int]].parse(Symbol("key"), "value") mustEqual (Symbol("key"), "Cannot parse key 'key with value value into integer").asLeft
    }

    "parse UUID values" in {
      ValueDecoder[UUID].parse(Symbol("key"), "") mustEqual (Symbol("key"), "Field 'key cannot be empty").asLeft
      ValueDecoder[UUID].parse(Symbol("key"), "d2161fd1-ffed-41df-ac3e-a729012105f5") mustEqual UUID.fromString("d2161fd1-ffed-41df-ac3e-a729012105f5").asRight
      ValueDecoder[UUID].parse(Symbol("key"), "value") mustEqual (Symbol("key"), "Cannot parse key 'key with value value into UUID").asLeft
    }

    "parse Option[Boolean] values" in {
      ValueDecoder[Option[Boolean]].parse(Symbol("key"), "") mustEqual None.asRight
      ValueDecoder[Option[Boolean]].parse(Symbol("key"), "0") mustEqual Some(false).asRight
      ValueDecoder[Option[Boolean]].parse(Symbol("key"), "1") mustEqual Some(true).asRight
      ValueDecoder[Option[Boolean]].parse(Symbol("key"), "value") mustEqual (Symbol("key"), "Cannot parse key 'key with value value into boolean").asLeft
    }

    "parse Option[Double] values" in {
      ValueDecoder[Option[Double]].parse(Symbol("key"), "") mustEqual None.asRight
      ValueDecoder[Option[Double]].parse(Symbol("key"), "42.5") mustEqual Some(42.5).asRight
      ValueDecoder[Option[Double]].parse(Symbol("key"), "value") mustEqual (Symbol("key"), "Cannot parse key 'key with value value into double").asLeft
    }

    "parse Instant and Option[Instant] values" in {
      ValueDecoder[Instant].parse(Symbol("key"), "") mustEqual (Symbol("key"), "Field 'key cannot be empty").asLeft
      ValueDecoder[Instant].parse(Symbol("key"), "2013-11-26 00:03:57.885") mustEqual Instant.parse("2013-11-26T00:03:57.885Z").asRight
      ValueDecoder[Instant].parse(Symbol("key"), "value") mustEqual (Symbol("key"), "Cannot parse key 'key with value value into datetime").asLeft
      ValueDecoder[Option[Instant]].parse(Symbol("key"), "") mustEqual None.asRight
      ValueDecoder[Option[Instant]].parse(Symbol("key"), "2013-11-26 00:03:57.885") mustEqual Some(Instant.parse("2013-11-26T00:03:57.885Z")).asRight
      ValueDecoder[Option[Instant]].parse(Symbol("key"), "value") mustEqual (Symbol("key"), "Cannot parse key 'key with value value into datetime").asLeft
    }

    "parse Contexts values" in {
      val validContexts =
        """{
        "schema": "iglu:com.snowplowanalytics.snowplow/contexts/jsonschema/1-0-0",
        "data": [
          {
            "schema": "iglu:org.schema/WebPage/jsonschema/1-0-0",
            "data": {
              "genre": "blog",
              "inLanguage": "en-US",
              "datePublished": "2014-11-06T00:00:00Z",
              "author": "Fred Blundun",
              "breadcrumb": [
                "blog",
                "releases"
              ],
              "keywords": [
                "snowplow",
                "javascript",
                "tracker",
                "event"
              ]
            }
          }
        ]
      }"""
      val invalidPayloadContexts =
        """{
        "schema": "iglu:invalid/schema/jsonschema/1-0-0",
        "data": [
          {
            "schema": "iglu:org.schema/WebPage/jsonschema/1-0-0",
            "data": {
              "genre": "blog",
              "inLanguage": "en-US",
              "datePublished": "2014-11-06T00:00:00Z",
              "author": "Fred Blundun",
              "breadcrumb": [
                "blog",
                "releases"
              ],
              "keywords": [
                "snowplow",
                "javascript",
                "tracker",
                "event"
              ]
            }
          }
        ]
      }"""
      ValueDecoder[Contexts].parse(Symbol("key"), "") mustEqual Contexts(List()).asRight
      ValueDecoder[Contexts].parse(Symbol("key"), validContexts) mustEqual Contexts(
        List(
          SelfDescribingData(
            SchemaKey(
              "org.schema",
              "WebPage",
              "jsonschema",
              SchemaVer.Full(1, 0, 0)
            ),
            JsonObject(
              ("genre", "blog".asJson),
              ("inLanguage", "en-US".asJson),
              ("datePublished", "2014-11-06T00:00:00Z".asJson),
              ("author", "Fred Blundun".asJson),
              ("breadcrumb", List("blog", "releases").asJson),
              ("keywords", List("snowplow", "javascript", "tracker", "event").asJson)
            ).asJson
          )
        )
      ).asRight
      ValueDecoder[Contexts].parse(Symbol("key"), invalidPayloadContexts) mustEqual (Symbol("key"), "Unknown payload: iglu:invalid/schema/jsonschema/1-0-0").asLeft
    }

    "parse UnstructEvent values" in {
      val validUnstruct =
        """{
        "schema": "iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0",
        "data": {
          "schema": "iglu:com.snowplowanalytics.snowplow/link_click/jsonschema/1-0-1",
          "data": {
            "targetUrl": "http://www.example.com",
            "elementClasses": ["foreground"],
            "elementId": "exampleLink"
          }
        }
      }"""
      val invalidPayloadUnstruct =
        """{
        "schema": "iglu:invalid/schema/jsonschema/1-0-0",
        "data": {
          "schema": "iglu:com.snowplowanalytics.snowplow/link_click/jsonschema/1-0-1",
          "data": {
            "targetUrl": "http://www.example.com",
            "elementClasses": ["foreground"],
            "elementId": "exampleLink"
          }
        }
      }"""
      ValueDecoder[UnstructEvent].parse(Symbol("key"), "") mustEqual UnstructEvent(None).asRight
      ValueDecoder[UnstructEvent].parse(Symbol("key"), validUnstruct) mustEqual UnstructEvent(
        Some(
          SelfDescribingData(
            SchemaKey(
              "com.snowplowanalytics.snowplow",
              "link_click",
              "jsonschema",
              SchemaVer.Full(1, 0, 1)
            ),
            JsonObject(
              ("targetUrl", "http://www.example.com".asJson),
              ("elementClasses", List("foreground").asJson),
              ("elementId", "exampleLink".asJson)
            ).asJson
          )
        )
      ).asRight
      ValueDecoder[UnstructEvent].parse(Symbol("key"), invalidPayloadUnstruct) mustEqual (Symbol("key"), "Unknown payload: iglu:invalid/schema/jsonschema/1-0-0").asLeft
    }
  }
}
