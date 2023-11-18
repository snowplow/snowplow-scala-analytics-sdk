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
import com.snowplowanalytics.snowplow.analytics.scalasdk.validate.FIELD_SIZES

import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.UUID
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

// cats
import cats.syntax.either._
import cats.syntax.option._
import cats.syntax.show._

// iglu
import com.snowplowanalytics.iglu.core.SelfDescribingData
import com.snowplowanalytics.iglu.core.circe.implicits._

// circe
import io.circe.jawn.JawnParser
import io.circe.{Error, Json, ParsingFailure}

// This library
import com.snowplowanalytics.snowplow.analytics.scalasdk.Common.{ContextsCriterion, UnstructEventCriterion}
import com.snowplowanalytics.snowplow.analytics.scalasdk.SnowplowEvent.{Contexts, UnstructEvent}
import com.snowplowanalytics.snowplow.analytics.scalasdk.ParsingError.RowDecodingErrorInfo
import com.snowplowanalytics.snowplow.analytics.scalasdk.ParsingError.RowDecodingErrorInfo._

private[decode] trait ValueDecoder[A] {
  def parse(
    key: Key,
    value: String,
    maxLength: Option[Int]
  ): DecodedValue[A]

  def parseBytes(
    key: Key,
    value: ByteBuffer,
    maxLength: Option[Int]
  ): DecodedValue[A] =
    parse(key, StandardCharsets.UTF_8.decode(value).toString, maxLength)
}

private[decode] object ValueDecoder {

  private val parser: JawnParser = new JawnParser

  def apply[A](implicit readA: ValueDecoder[A]): ValueDecoder[A] = readA

  def fromFunc[A](f: ((Key, String, Option[Int])) => DecodedValue[A]): ValueDecoder[A] =
    new ValueDecoder[A] {
      def parse(
        key: Key,
        value: String,
        maxLength: Option[Int]
      ): DecodedValue[A] = f((key, value, maxLength))
    }

  implicit final val stringColumnDecoder: ValueDecoder[String] =
    fromFunc[String] {
      case (key, value, Some(maxLength)) if value.length > maxLength =>
        value.substring(0, maxLength).asRight
      case (key, "", _) =>
        InvalidValue(key, "", s"Field ${key.name} cannot be empty").asLeft
      case (_, value, _) =>
        value.asRight
    }

  implicit final val stringOptionColumnDecoder: ValueDecoder[Option[String]] =
    fromFunc[Option[String]] {
      case (key, value, Some(maxLength)) if value.length > maxLength =>
        value.substring(0, maxLength).some.asRight
      case (_, "", _) =>
        none[String].asRight
      case (_, value, _) =>
        value.some.asRight

    }

  implicit final val intColumnDecoder: ValueDecoder[Option[Int]] =
    fromFunc[Option[Int]] {
      case (key, value, _) =>
        if (value.isEmpty) none[Int].asRight
        else
          try value.toInt.some.asRight
          catch {
            case _: NumberFormatException =>
              InvalidValue(key, value, s"Cannot parse key ${key.name} into integer").asLeft
          }
    }

  implicit final val uuidColumnDecoder: ValueDecoder[UUID] =
    fromFunc[UUID] {
      case (key, value, _) =>
        if (value.isEmpty)
          InvalidValue(key, value, s"Field ${key.name} cannot be empty").asLeft
        else
          try UUID.fromString(value).asRight[RowDecodingErrorInfo]
          catch {
            case _: IllegalArgumentException =>
              InvalidValue(key, value, s"Cannot parse key ${key.name} into UUID").asLeft
          }
    }

  implicit final val boolColumnDecoder: ValueDecoder[Option[Boolean]] =
    fromFunc[Option[Boolean]] {
      case (key, value, _) =>
        value match {
          case "0" => false.some.asRight
          case "1" => true.some.asRight
          case "" => none[Boolean].asRight
          case _ => InvalidValue(key, value, s"Cannot parse key ${key.name} into boolean").asLeft
        }
    }

  implicit final val doubleColumnDecoder: ValueDecoder[Option[Double]] =
    fromFunc[Option[Double]] {
      case (key, value, _) =>
        if (value.isEmpty)
          none[Double].asRight
        else
          try value.toDouble.some.asRight
          catch {
            case _: NumberFormatException =>
              InvalidValue(key, value, s"Cannot parse key ${key.name} into double").asLeft
          }
    }

  implicit final val instantColumnDecoder: ValueDecoder[Instant] =
    fromFunc[Instant] {
      case (key, value, _) =>
        if (value.isEmpty)
          InvalidValue(key, value, s"Field ${key.name} cannot be empty").asLeft
        else {
          val tstamp = reformatTstamp(value)
          try Instant.parse(tstamp).asRight
          catch {
            case _: DateTimeParseException =>
              InvalidValue(key, value, s"Cannot parse key ${key.name} into datetime").asLeft
          }
        }
    }

  implicit final val instantOptionColumnDecoder: ValueDecoder[Option[Instant]] =
    fromFunc[Option[Instant]] {
      case (key, value, _) =>
        if (value.isEmpty)
          none[Instant].asRight[RowDecodingErrorInfo]
        else {
          val tstamp = reformatTstamp(value)
          try Instant.parse(tstamp).some.asRight
          catch {
            case _: DateTimeParseException =>
              InvalidValue(key, value, s"Cannot parse key ${key.name} into datetime").asLeft
          }
        }
    }

  implicit final val unstructuredJson: ValueDecoder[UnstructEvent] = {
    def fromJsonParseResult(
      result: Either[ParsingFailure, Json],
      key: Key,
      originalValue: => String
    ): DecodedValue[UnstructEvent] = {
      def asLeft(error: Error): RowDecodingErrorInfo = InvalidValue(key, originalValue, error.show)
      result
        .flatMap(_.as[SelfDescribingData[Json]])
        .leftMap(asLeft) match {
        case Right(SelfDescribingData(schema, data)) if UnstructEventCriterion.matches(schema) =>
          data.as[SelfDescribingData[Json]].leftMap(asLeft).map(_.some).map(UnstructEvent.apply)
        case Right(SelfDescribingData(schema, _)) =>
          InvalidValue(key, originalValue, s"Unknown payload: ${schema.toSchemaUri}").asLeft[UnstructEvent]
        case Left(error) => error.asLeft[UnstructEvent]
      }
    }
    new ValueDecoder[UnstructEvent] {
      def parse(
        key: Key,
        value: String,
        maxLength: Option[Int]
      ): DecodedValue[UnstructEvent] =
        if (value.isEmpty)
          UnstructEvent(None).asRight[RowDecodingErrorInfo]
        else
          fromJsonParseResult(parser.parse(value), key, value)

      override def parseBytes(
        key: Key,
        value: ByteBuffer,
        maxLength: Option[Int]
      ): DecodedValue[UnstructEvent] =
        if (!value.hasRemaining())
          UnstructEvent(None).asRight[RowDecodingErrorInfo]
        else
          fromJsonParseResult(parser.parseByteBuffer(value), key, StandardCharsets.UTF_8.decode(value).toString)
    }
  }

  implicit final val contexts: ValueDecoder[Contexts] = {
    def fromJsonParseResult(
      result: Either[ParsingFailure, Json],
      key: Key,
      originalValue: => String
    ): DecodedValue[Contexts] = {
      def asLeft(error: Error): RowDecodingErrorInfo = InvalidValue(key, originalValue, error.show)
      result
        .flatMap(_.as[SelfDescribingData[Json]])
        .leftMap(asLeft) match {
        case Right(SelfDescribingData(schema, data)) if ContextsCriterion.matches(schema) =>
          data.as[List[SelfDescribingData[Json]]].leftMap(asLeft).map(Contexts.apply)
        case Right(SelfDescribingData(schema, _)) =>
          InvalidValue(key, originalValue, s"Unknown payload: ${schema.toSchemaUri}").asLeft[Contexts]
        case Left(error) => error.asLeft[Contexts]
      }
    }
    new ValueDecoder[Contexts] {
      def parse(
        key: Key,
        value: String,
        maxLength: Option[Int]
      ): DecodedValue[Contexts] =
        if (value.isEmpty)
          Contexts(List.empty).asRight[RowDecodingErrorInfo]
        else
          fromJsonParseResult(parser.parse(value), key, value)

      override def parseBytes(
        key: Key,
        value: ByteBuffer,
        maxLength: Option[Int]
      ): DecodedValue[Contexts] =
        if (!value.hasRemaining())
          Contexts(List.empty).asRight[RowDecodingErrorInfo]
        else
          fromJsonParseResult(parser.parseByteBuffer(value), key, StandardCharsets.UTF_8.decode(value).toString)
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
