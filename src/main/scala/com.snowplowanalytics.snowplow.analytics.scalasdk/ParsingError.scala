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

/**
  * Represents error during parsing TSV event
  */
sealed trait ParsingError

object ParsingError {

  /**
    * Represents error which given line is not a TSV
    */
  final case object NonTSVPayload extends ParsingError

  /**
    * Represents error which number of given columns is not equal
    * to number of expected columns
    * @param columnCount mismatched column count in the event
    */
  final case class ColumnNumberMismatch(columnCount: Int) extends ParsingError

  /**
    * Represents error which encountered while decoding values in row
    * @param errors Infos of errors which encountered during decoding
    */
  final case class RowDecodingError(errors: NonEmptyList[RowDecodingErrorInfo]) extends ParsingError

  /**
    * Gives info about the reasons of the errors during decoding value in row
    */
  sealed trait RowDecodingErrorInfo

  object RowDecodingErrorInfo {
    /**
      * Represents cases where value in a field is not valid
      * e.g invalid timestamp, invalid UUID
      * @param key key of field
      * @param value value of field
      * @param message error message
      */
    final case class InvalidValue(key: Key, value: String, message: String) extends RowDecodingErrorInfo

    /**
      * Represents cases which getting error is not expected while decoding row
      * For example, while parsing the list of tuples to HList in the
      * RowDecoder, getting more or less values than expected is impossible
      * due to type check. Therefore 'UnexpectedRowDecodingError' is returned for
      * these cases. These errors can be ignored since they are not possible to get
      * @param error error message
      */
    final case class UnexpectedRowDecodingError(error: String) extends RowDecodingErrorInfo

    implicit val analyticsSdkRowDecodingErrorInfoCirceEncoder: Encoder[RowDecodingErrorInfo] =
      Encoder.instance {
        case InvalidValue(key, value, message) =>
          Json.obj(
            "type" := "InvalidValue",
            "key" := key,
            "value" := value,
            "message" := message
          )
        case UnexpectedRowDecodingError(error: String) =>
          Json.obj(
            "type" := "UnexpectedRowDecodingError",
            "error" := error
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

            case "UnexpectedRowDecodingError" =>
              cursor
                .downField("error")
                .as[String]
                .map(UnexpectedRowDecodingError)
          }
        } yield result
      }

    implicit val analyticsSdkKeyCirceEncoder: Encoder[Key] =
      Encoder.instance(_.toString.stripPrefix("'").asJson)

    implicit val analyticsSdkKeyCirceDecoder: Decoder[Key] =
      Decoder.instance(_.as[String].map(Symbol(_)))

  }

  implicit val analyticsSdkParsingErrorCirceEncoder: Encoder[ParsingError] =
    Encoder.instance {
      case NonTSVPayload =>
        Json.obj("type" := "NonTSVPayload")
      case ColumnNumberMismatch(columnCount) =>
        Json.obj(
          "type" := "ColumnNumberMismatch",
          "columnCount" := columnCount
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
            case "NonTSVPayload" =>
              NonTSVPayload.asRight
            case "ColumnNumberMismatch" =>
              cursor
                .downField("columnCount")
                .as[Int]
                .map(ColumnNumberMismatch)
            case "RowDecodingError" =>
              cursor
                .downField("errors")
                .as[NonEmptyList[RowDecodingErrorInfo]]
                .map(RowDecodingError)
            case _ =>
              DecodingFailure(
                s"Error type $error cannot be recognized as Analytics SDK Parsing Error",
                cursor.history).asLeft
          }
        } yield result
      }
}
