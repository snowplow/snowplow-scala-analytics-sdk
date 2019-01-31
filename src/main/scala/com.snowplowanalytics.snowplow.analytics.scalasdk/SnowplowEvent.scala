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

// java
import java.util.UUID

// circe
import io.circe.syntax._
import io.circe.{Encoder, Json, JsonObject}

// iglu
import com.snowplowanalytics.iglu.core.{SchemaKey, SchemaVer, SelfDescribingData}
import com.snowplowanalytics.iglu.core.circe.CirceIgluCodecs._

trait SnowplowEvent {
  def event_id: UUID
}

object SnowplowEvent {

  /**
    * A JSON representation of an atomic event's unstruct_event field.
    *
    * @param data the unstruct event as self-describing JSON, or None if the field is missing
    */
  case class UnstructEvent(data: Option[SelfDescribingData[Json]]) extends AnyVal {
    def toJson: Option[(String, Json)] = {
      data.map {
        case SelfDescribingData(s, d) =>
          (transformSchema("unstruct_event", s.vendor, s.name, s.version.model), d)
      }
    }
  }
  val UnstructEventUri = SchemaKey("com.snowplowanalytics.snowplow", "unstruct_event", "jsonschema", SchemaVer.Full(1, 0, 0))

  /**
    * A JSON representation of an atomic event's contexts or derived_contexts fields.
    *
    * @param data the context as self-describing JSON, or None if the field is missing
    */
  case class Contexts(data: List[SelfDescribingData[Json]]) extends AnyVal {
    def toJson: Map[String, Json] = {
      data.groupBy(x => (x.schema.vendor, x.schema.name, x.schema.format, x.schema.version.model)).map {
        case ((vendor, name, _, model), d) =>
          (transformSchema("contexts", vendor, name, model), d.map{selfdesc => selfdesc.data}.asJson)
      }
    }
  }
  val ContextsUri = SchemaKey("com.snowplowanalytics.snowplow", "contexts", "jsonschema", SchemaVer.Full(1, 0, 0))

  implicit final val contextsCirceEncoder: io.circe.Encoder[Contexts] =
    Encoder.instance { contexts: Contexts =>
      if (contexts.data.isEmpty) List[String]().asJson
      else JsonObject(
        ("schema", ContextsUri.toSchemaUri.asJson),
        ("data", contexts.data.asJson)
      ).asJson
    }

  implicit final val unstructCirceEncoder: io.circe.Encoder[UnstructEvent] =
    Encoder.instance { unstructEvent: UnstructEvent =>
      if (unstructEvent.data.isEmpty) Json.Null
      else JsonObject(
        ("schema", UnstructEventUri.toSchemaUri.asJson),
        ("data", unstructEvent.data.asJson)
      ).asJson
    }

  /**
    * @param prefix "contexts" or "unstruct_event"
    * @param vendor Iglu schema vendor
    * @param name   Iglu schema name
    * @param model  Iglu schema model
    * @return       the schema, transformed into an Elasticsearch-compatible column name
    */
  def transformSchema(prefix: String, vendor: String, name: String, model: Int): String = {
    // Convert dots & dashes in schema vendor to underscore
    val snakeCaseVendor = vendor.replaceAll("""[\.\-]""", "_").toLowerCase

    // Convert PascalCase in schema name to snake_case
    val snakeCaseName = name.replaceAll("""[\.\-]""", "_").replaceAll("([^A-Z_])([A-Z])", "$1_$2").toLowerCase

    s"${prefix}_${snakeCaseVendor}_${snakeCaseName}_$model"
  }

  def transformSchema(prefix: String, schema: SchemaKey): String = transformSchema(prefix, schema.vendor, schema.name, schema.version.model)
}
