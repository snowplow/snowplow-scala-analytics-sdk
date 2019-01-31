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

// java
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.UUID

// cats
import cats.syntax.either._
import cats.syntax.option._
import cats.syntax.show._

// iglu
import com.snowplowanalytics.iglu.core.SelfDescribingData
import com.snowplowanalytics.iglu.core.circe.instances._

// circe
import io.circe.parser.{parse => parseJson}
import io.circe.{Error, Json}

// This library
import com.snowplowanalytics.snowplow.analytics.scalasdk.Common.{ContextsCriterion, UnstructEventCriterion}
import com.snowplowanalytics.snowplow.analytics.scalasdk.SnowplowEvent.{Contexts, UnstructEvent}

private[decode] trait ValueDecoder[A] {
  def parse(column: (Key, String)): DecodedValue[A]
}

private[decode] object ValueDecoder {
  def apply[A](implicit readA: ValueDecoder[A]): ValueDecoder[A] = readA

  def fromFunc[A](f: ((Key, String)) => DecodedValue[A]): ValueDecoder[A] =
    new ValueDecoder[A] {
      def parse(field: (Key, String)): DecodedValue[A] = f(field)
    }

  implicit final val stringColumnDecoder: ValueDecoder[String] =
    fromFunc[String] {
      case (key, value) =>
        if (value.isEmpty) (key, s"Field $key cannot be empty").asLeft else value.asRight
    }

  implicit final val stringOptionColumnDecoder: ValueDecoder[Option[String]] =
    fromFunc[Option[String]] {
      case (_, value) =>
        if (value.isEmpty) none[String].asRight else value.some.asRight
    }

  implicit final val intColumnDecoder: ValueDecoder[Option[Int]] =
    fromFunc[Option[Int]] {
      case (key, value) =>
        if (value.isEmpty) none[Int].asRight
        else try {
          value.toInt.some.asRight
        } catch {
          case _: NumberFormatException =>
            (key, s"Cannot parse key $key with value $value into integer").asLeft
        }
    }

  implicit final val uuidColumnDecoder: ValueDecoder[UUID] =
    fromFunc[UUID] {
      case (key, value) =>
        if (value.isEmpty)
          (key, s"Field $key cannot be empty").asLeft
        else
          try {
            UUID.fromString(value).asRight[(Key, String)]
          } catch {
            case _: IllegalArgumentException =>
              (key, s"Cannot parse key $key with value $value into UUID").asLeft
          }
    }

  implicit final val boolColumnDecoder: ValueDecoder[Option[Boolean]] =
    fromFunc[Option[Boolean]] {
      case (key, value) =>
        value match {
          case "0" => false.some.asRight
          case "1" => true.some.asRight
          case ""  => none[Boolean].asRight
          case _   => (key, s"Cannot parse key $key with value $value into boolean").asLeft
        }
    }

  implicit final val doubleColumnDecoder: ValueDecoder[Option[Double]] =
    fromFunc[Option[Double]] {
      case (key, value) =>
        if (value.isEmpty)
          none[Double].asRight
        else try {
          value.toDouble.some.asRight
        } catch {
          case _: NumberFormatException =>
            (key, s"Cannot parse key $key with value $value into double").asLeft
        }
    }

  implicit final val instantColumnDecoder: ValueDecoder[Instant] =
    fromFunc[Instant] {
      case (key, value) =>
        if (value.isEmpty)
          (key, s"Field $key cannot be empty").asLeft
        else {
          val tstamp = reformatTstamp(value)
          try {
            Instant.parse(tstamp).asRight
          } catch {
            case _: DateTimeParseException =>
              (key, s"Cannot parse key $key with value $value into datetime").asLeft
          }
        }
    }

  implicit final val instantOptionColumnDecoder: ValueDecoder[Option[Instant]] =
    fromFunc[Option[Instant]] {
      case (key, value) =>
      if (value.isEmpty)
        none[Instant].asRight[(Key, String)]
      else {
        val tstamp = reformatTstamp(value)
        try {
          Instant.parse(tstamp).some.asRight
        } catch {
          case _: DateTimeParseException =>
            (key, s"Cannot parse key $key with value $value into datetime").asLeft
        }
      }
    }

  implicit final val unstructuredJson: ValueDecoder[UnstructEvent] =
    fromFunc[UnstructEvent] {
      case (key, value) =>
        def asLeft(error: Error): (Key, String) = (key, error.show)
        if (value.isEmpty)
          UnstructEvent(None).asRight[(Key, String)]
        else
          parseJson(value)
            .flatMap(_.as[SelfDescribingData[Json]])
            .leftMap(asLeft) match {
            case Right(SelfDescribingData(schema, data)) if UnstructEventCriterion.matches(schema) =>
              data.as[SelfDescribingData[Json]].leftMap(asLeft).map(_.some).map(UnstructEvent.apply)
            case Right(SelfDescribingData(schema, _)) =>
              (key, s"Unknown payload: ${schema.toSchemaUri}").asLeft[UnstructEvent]
            case Left(error) => error.asLeft[UnstructEvent]
          }
    }

  implicit final val contexts: ValueDecoder[Contexts] =
    fromFunc[Contexts] {
      case (key, value) =>
        def asLeft(error: Error): (Key, String) = (key, error.show)
        if (value.isEmpty)
          Contexts(List()).asRight[(Key, String)]
        else
          parseJson(value)
            .flatMap(_.as[SelfDescribingData[Json]])
            .leftMap(asLeft) match {
            case Right(SelfDescribingData(schema, data)) if ContextsCriterion.matches(schema) =>
              data.as[List[SelfDescribingData[Json]]].leftMap(asLeft).map(Contexts.apply)
            case Right(SelfDescribingData(schema, _)) =>
              (key, s"Unknown payload: ${schema.toSchemaUri}").asLeft[Contexts]
            case Left(error) => error.asLeft[Contexts]
          }
    }

  /**
    * Converts a timestamp to an ISO-8601 format usable by Instant.parse()
    *
    * @param tstamp Timestamp of the form YYYY-MM-DD hh:mm:ss
    * @return ISO-8601 timestamp
    */
  private def reformatTstamp(tstamp: String): String = tstamp.replaceAll(" ", "T") + "Z"
}

