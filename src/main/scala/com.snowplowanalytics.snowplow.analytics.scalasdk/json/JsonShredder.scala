 /*
 * Copyright (c) 2016-2017 Snowplow Analytics Ltd.
 * All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache
 * License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 *
 * See the Apache License Version 2.0 for the specific language
 * governing permissions and limitations there under.
 */
package com.snowplowanalytics.snowplow
package analytics.scalasdk
package json

// json4s
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._

// Scala
import scala.annotation.tailrec

/**
 * Converts unstructured events and custom contexts to a format which the Elasticsearch
 * mapper can understand
 */
object JsonShredder {

  /**
    * ADT representing field types that can be shredded (self-describing JSONs)
    */
  sealed trait ShredPrefix { def asString: String }
  case object Contexts extends ShredPrefix { def asString = "contexts" }
  case object UnstructEvent extends ShredPrefix { def asString = "unstruct_event" }

  /**
    * Canonical Iglu Schema URI regiex
    * TODO: replace with Iglu core
    */
  val schemaPattern = (
    "^iglu:" +                            // Protocol
      "([a-zA-Z0-9-_.]+)/" +              // Vendor
      "([a-zA-Z0-9-_]+)/" +               // Name
      "([a-zA-Z0-9-_]+)/" +               // Format
      "([1-9][0-9]*" +                    // MODEL (cannot start with 0)
      "(?:-(?:0|[1-9][0-9]*)){2})$").r    // REVISION and ADDITION
                                          // Extract whole SchemaVer within single group

  /**
   * Create an Elasticsearch field name from a schema
   *
   * "iglu:com.acme/PascalCase/jsonschema/13-0-0" -> "context_com_acme_pascal_case_13"
   *
   * @param prefix "context" or "unstruct_event"
   * @param schema Schema field from an incoming JSON
   * @return Elasticsearch field name
   */
   // TODO: move this to shared storage/shredding utils
   // See https://github.com/snowplow/snowplow/issues/1189
  def fixSchema(prefix: ShredPrefix, schema: String): Either[String, String] = {
    schema match {
      case schemaPattern(organization, name, _, schemaVer) =>
        // Split the vendor's reversed domain name using underscores rather than dots
        val snakeCaseOrganization = organization.replaceAll("""[\.\-]""", "_").toLowerCase

        // Change the name from PascalCase or lisp-case to snake_case if necessary
        val snakeCaseName = name.replaceAll("""[\.\-]""", "_").replaceAll("([^A-Z_])([A-Z])", "$1_$2").toLowerCase

        // Extract the schemaver version's model
        val model = schemaVer.split("-")(0)

        Right(s"${prefix.asString}_${snakeCaseOrganization}_${snakeCaseName}_$model")
      case _ => Left(s"Schema [$schema] does not conform to Iglu Schema URI regular expression")
    }
  }

  /**
   * Convert a contexts JSON to an Elasticsearch-compatible JObject
   * For example, the JSON
   *
   *  {
   *    "schema": "iglu:com.snowplowanalytics.snowplow/contexts/jsonschema/1-0-0",
   *    "data": [
   *      {
   *        "schema": "iglu:com.acme/unduplicated/jsonschema/1-0-0",
   *        "data": {
   *          "unique": true
   *        }
   *      },
   *      {
   *        "schema": "iglu:com.acme/duplicated/jsonschema/1-0-0",
   *        "data": {
   *          "value": 1
   *        }
   *      },
   *      {
   *        "schema": "iglu:com.acme/duplicated/jsonschema/1-0-0",
   *        "data": {
   *          "value": 2
   *        }
   *      }
   *    ]
   *  }
   *
   * would become
   *
   *  {
   *    "context_com_acme_duplicated_1": [{"value": 1}, {"value": 2}],
   *    "context_com_acme_unduplicated_1": [{"unique": true}]
   *  }
   *
   * @param contexts Contexts JSON
   * @return Contexts JSON in an Elasticsearch-compatible format
   */
  def parseContexts(contexts: String): Either[List[String], JObject] = {

    /**
     * Validates and pairs up the schema and data fields without grouping the same schemas together
     *
     * For example, the JSON
     *
     *  {
     *    "schema": "iglu:com.snowplowanalytics.snowplow/contexts/jsonschema/1-0-0",
     *    "data": [
     *      {
     *        "schema": "iglu:com.acme/duplicated/jsonschema/1-0-0",
     *        "data": {
     *          "value": 1
     *        }
     *      },
     *      {
     *        "schema": "iglu:com.acme/duplicated/jsonschema/1-0-0",
     *        "data": {
     *          "value": 2
     *        }
     *      }
     *    ]
     *  }
     *
     * would become
     *
     * [
     *   {"context_com_acme_duplicated_1": {"value": 1}},
     *   {"context_com_acme_duplicated_1": {"value": 2}}
     * ]
     *
     * @param contextJsons List of inner custom context JSONs
     * @param accumulator Custom contexts which have already been parsed
     * @return List of validated tuples containing a fixed schema string and the original data JObject
     */
    @tailrec def innerParseContexts(contextJsons: List[JValue], accumulator: List[Either[List[String], JField]]):
    List[Either[List[String], JField]] = {

      contextJsons match {
        case Nil => accumulator
        case context :: tail =>
          val innerData = context \ "data" match {
            case JNothing => Left("Could not extract inner data field from custom context")
            case d => Right(d)
          }
          val fixedSchema: Either[String, String] = context \ "schema" match {
            case JString(schema) => fixSchema(Contexts, schema)
            case _ => Left("Context JSON did not contain a stringly typed schema field")
          }

          val schemaDataPair = fixedSchema.map2(innerData) {_ -> _}

          innerParseContexts(tail, schemaDataPair :: accumulator)
      }
    }

    val json = parse(contexts)
    val data = json \ "data"

    data match {
      case JArray(jsons) =>
        val innerContexts = innerParseContexts(jsons, Nil).traverseEitherL
        // Group contexts with the same schema together
        innerContexts.map(_.groupBy(_._1).map(pair => (pair._1, pair._2.map(_._2))))
      case _ => Left(List("Could not extract contexts data field as an array"))
    }
  }

  /**
   * Convert an unstructured event JSON to an Elasticsearch-compatible JObject
   * For example, the JSON
   *
   *  {
   *    "schema": "iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0",
   *    "data": {
   *      "schema": "iglu:com.snowplowanalytics.snowplow/link_click/jsonschema/1-0-1",
   *      "data": {
   *        "key": "value"
   *      }
   *    }
   *  }
   *
   * would become
   *
   *  {
   *    "unstruct_com_snowplowanalytics_snowplow_link_click_1": {"key": "value"}
   *  }
   *
   * @param unstruct Unstructured event JSON
   * @return Unstructured event JSON in an Elasticsearch-compatible format
   */
  def parseUnstruct(unstruct: String): Either[List[String], JObject] = {
    val json = parse(unstruct)
    val data = json \ "data"
    val schema = data \ "schema"
    val innerData: Either[String, JValue] = data \ "data" match {
      case JNothing => Left("Could not extract inner data field from unstructured event") // TODO: decide whether to enforce object type of data
      case d => Right(d)
    }
    val fixedSchema = schema match {
      case JString(s) => fixSchema(UnstructEvent, s)
      case _ => Left("Unstructured event JSON did not contain a stringly typed schema field")
    }

    fixedSchema.map2(innerData) {_ -> _}
  }
}
