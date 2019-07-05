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

import cats.data.NonEmptyList
import cats.syntax.either._
import io.circe._
import io.circe.syntax._
import com.snowplowanalytics.snowplow.analytics.scalasdk.decode.Key
import com.snowplowanalytics.snowplow.analytics.scalasdk.decode.Key._

/**
  * Represents an error raised when parsing a TSV line.
  */
sealed trait ParsingError

object ParsingError {

  /**
    * Represents an error indicating a non-TSV line.
    */
  final case object NotTSV extends ParsingError

  /**
    * Represents an error indicating the number of actual fields is not equal
    * to the number of expected fields.
    * @param fieldCount The number of fields in the TSV line.
    */
  final case class FieldNumberMismatch(fieldCount: Int) extends ParsingError

  /**
    * Represents an error raised when trying to decode the values in a line.
    * @param errors A non-empty list of errors encountered when trying to decode the values.
    */
  final case class RowDecodingError(errors: NonEmptyList[RowDecodingErrorInfo]) extends ParsingError

  /**
    * Contains information about the reasons behind errors raised when trying to decode the values in a line.
    */
  sealed trait RowDecodingErrorInfo

  object RowDecodingErrorInfo {
    /**
      * Represents cases where tha value in a field is not valid,
      * e.g. an invalid timestamp, an invalid UUID, etc.
      * @param key The name of the field.
      * @param value The value of field.
      * @param message The error message.
      */
    final case class InvalidValue(key: Key, value: String, message: String) extends RowDecodingErrorInfo

    /**
      * Represents unhandled errors raised when trying to decode a line.
      * For example, while parsing a list of tuples to [[HList]] in
      * [[RowDecoder]], type checking should make it impossible to get more or less values
      * than expected.
      * @param message The error message.
      */
    final case class UnhandledRowDecodingError(message: String) extends RowDecodingErrorInfo

    implicit val analyticsSdkRowDecodingErrorInfoCirceEncoder: Encoder[RowDecodingErrorInfo] =
      Encoder.instance {
        case InvalidValue(key, value, message) =>
          Json.obj(
            "type" := "InvalidValue",
            "key" := key,
            "value" := value,
            "message" := message
          )
        case UnhandledRowDecodingError(message: String) =>
          Json.obj(
            "type" := "UnhandledRowDecodingError",
            "message" := message
          )
      }

    implicit val analyticsSdkRowDecodingErrorInfoCirceDecoder: Decoder[RowDecodingErrorInfo] =
      Decoder.instance { cursor =>
        for {
          errorType <- cursor.downField("type").as[String]
          result <- errorType match {
            case "InvalidValue" =>
              for {
                key <- cursor.downField("key").as[Key]
                value <- cursor.downField("value").as[String]
                message <- cursor.downField("message").as[String]
              } yield InvalidValue(key, value, message)

            case "UnhandledRowDecodingError" =>
              cursor
                .downField("message")
                .as[String]
                .map(UnhandledRowDecodingError)
          }
        } yield result
      }
  }

  implicit val analyticsSdkParsingErrorCirceEncoder: Encoder[ParsingError] =
    Encoder.instance {
      case NotTSV =>
        Json.obj("type" := "NotTSV")
      case FieldNumberMismatch(fieldCount) =>
        Json.obj(
          "type" := "FieldNumberMismatch",
          "fieldCount" := fieldCount
        )
      case RowDecodingError(errors) =>
        Json.obj(
          "type" := "RowDecodingError",
          "errors" := errors.asJson
        )
    }

    implicit val analyticsSdkParsingErrorCirceDecoder: Decoder[ParsingError] =
      Decoder.instance { cursor =>
        for {
          error <- cursor.downField("type").as[String]
          result <- error match {
            case "NotTSV" =>
              NotTSV.asRight
            case "FieldNumberMismatch" =>
              cursor
                .downField("fieldCount")
                .as[Int]
                .map(FieldNumberMismatch)
            case "RowDecodingError" =>
              cursor
                .downField("errors")
                .as[NonEmptyList[RowDecodingErrorInfo]]
                .map(RowDecodingError)
            case _ =>
              DecodingFailure(
                s"Error type $error is not an Analytics SDK Parsing Error.",
                cursor.history).asLeft
          }
        } yield result
      }
}
