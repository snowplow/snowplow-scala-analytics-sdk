 /*
 * Copyright (c) 2016-2018 Snowplow Analytics Ltd.
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

// Scala
import scala.annotation.tailrec

// This library
import Data._

/**
 * Converts unstructured events and custom contexts to a format which the Elasticsearch
 * mapper can understand
 */
object JsonShredder {

  /**
    * Canonical Iglu Schema URI regex
    * TODO: replace with Iglu core: https://github.com/snowplow/snowplow-scala-analytics-sdk/issues/38
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
   * Convert a contexts JSON to an Elasticsearch-compatible JObject enveloped in `ContextOutput`
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
   *      },
   *      {
   *        "schema": "iglu:com.acme/duplicated/jsonschema/1-0-1",
   *        "data": {
   *          "value": 3
   *        }
   *      }
   *    ]
   *  }
   *
   * would become
   *
   *  {
   *    "iglu:com.acme/duplicated/jsonschema/1-0-0": [{"value": 1}, {"value": 2}],
   *    "iglu:com.acme/duplicated/jsonschema/1-0-1": [{"value": 3}],
   *    "iglu:com.acme/unduplicated/jsonschema/1-0-0": [{"unique": true}]
   *  }
   *
   * NOTE: it does not merge contexts on per-model basis (as we can see from 1st and 2nd items above)
   *
   * @param contextsType contexts flavor (derived or custom)
   * @param contexts Contexts JSON
   * @return Contexts JSON in an Elasticsearch-compatible format
   */
  def parseContexts(contextsType: ContextsType)(contexts: String): Validated[ContextsOutput] = {

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
     *   {"iglu:com.acme/duplicated/jsonschema/1-0-0": {"value": 1}},
     *   {"iglu:com.acme/duplicated/jsonschema/1-0-0": {"value": 2}}
     * ]
     *
     * @param contextJsons List of inner custom context JSONs
     * @param accumulator Custom contexts which have already been parsed
     * @return List of validated tuples containing a fixed schema string and the original data JObject
     */
    @tailrec def innerParseContexts(
      contextJsons: List[JValue],
      accumulator: List[Either[List[String], JField]]
    ): List[Either[List[String], JField]] = {

      contextJsons match {
        case Nil => accumulator
        case context :: tail =>
          val innerData = context \ "data" match {
            case JNothing => Left("Could not extract inner data field from custom context")
            case d => Right(d)
          }
          val fixedSchema = context \ "schema" match {
            case JString(schema) if schemaPattern.pattern.matcher(schema).matches => Right(schema)
            case JString(schema) => Left(s"Schema [$schema] does not conform to Iglu Schema URI regular expression")
            case _ => Left("Context JSON did not contain a stringly typed schema field")
          }

          val schemaDataPair = fixedSchema.map2(innerData) {_ -> _}

          innerParseContexts(tail, schemaDataPair :: accumulator)
      }
    }

    val result = parse(contexts) \ "data" match {
      case JArray(jsons) =>
        val innerContexts = innerParseContexts(jsons, Nil).traverseEitherL
        innerContexts.map(groupJsons)
      case _ => Left(List("Could not extract contexts data field as an array"))
    }

    result.map(map => ContextsOutput(contextsType, map))
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
   *    "iglu:com.snowplowanalytics.snowplow/link_click/jsonschema/1-0-1": {"key": "value"}
   *  }
   *
   * @param unstruct Unstructured event JSON
   * @return Unstructured event JSON in an Elasticsearch-compatible format
   */
  def parseUnstruct(unstruct: String): Validated[UnstructEventOutput] = {
    val json = parse(unstruct)
    val data = json \ "data"
    val schema = data \ "schema"
    val innerData: Either[String, JValue] = data \ "data" match {
      case JNothing => Left("Could not extract inner data field from unstructured event") // TODO: decide whether to enforce object type of data
      case d => Right(d)
    }
    val fixedSchema = schema match {
      case JString(schema) if schemaPattern.pattern.matcher(schema).matches => Right(schema)
      case JString(schema) => Left(s"Schema [$schema] does not conform to Iglu Schema URI regular expression")
      case _ => Left("Unstructured event JSON did not contain a stringly typed schema field")
    }

    val result = fixedSchema.map2(innerData) {_ -> _}
    result.map { case (igluUri, json) => UnstructEventOutput(igluUri, json) }
  }

  /**
   * Group list of schema-data pairs into Map with data grouped into list by its schema
   */
  private def groupJsons(schemaPairs: List[(IgluUri, JValue)]): Map[IgluUri, List[JValue]] =
    schemaPairs.groupBy(_._1).map { case (igluUri, pairs) => (igluUri, pairs.map(_._2)) }
}
