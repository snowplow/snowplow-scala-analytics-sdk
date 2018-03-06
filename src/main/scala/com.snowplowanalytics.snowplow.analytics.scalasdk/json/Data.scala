/*
 * Copyright (c) 2016-2018 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.snowplow.analytics.scalasdk.json

// Json4s
import org.json4s._

/**
 * Common data types for enriched event
 */
object Data {

  /**
   * Type-alias to mark string as valid Iglu URI
   * TODO: replace with Iglu core: https://github.com/snowplow/snowplow-scala-analytics-sdk/issues/38
   */
  type IgluUri = String

  /**
   * Elasticsearch-compatible field name for shredded type
   * e.g. unstruct_event_some_custom_event_1
   */
  type ShredFieldName = String

  /**
   * Transform Iglu URI into elasticsearch-compatible column name
   *
   * "iglu:com.acme/PascalCase/jsonschema/13-0-0" -> "context_com_acme_pascal_case_13"
   *
   * @param shredProperty "context" or "unstruct_event"
   * @param igluUri Schema field from an incoming JSON, should be already validated
   * @return Elasticsearch field name
   */
  def fixSchema(shredProperty: ShredProperty, igluUri: IgluUri): ShredFieldName = {
    igluUri match {
      case JsonShredder.schemaPattern(organization, name, _, schemaVer) =>
        // Split the vendor's reversed domain name using underscores rather than dots
        val snakeCaseOrganization = organization.replaceAll("""[\.\-]""", "_").toLowerCase

        // Change the name from PascalCase or lisp-case to snake_case if necessary
        val snakeCaseName = name.replaceAll("""[\.\-]""", "_").replaceAll("([^A-Z_])([A-Z])", "$1_$2").toLowerCase

        // Extract the schemaver version's model
        val model = schemaVer.split("-")(0)

        s"${shredProperty.prefix}${snakeCaseOrganization}_${snakeCaseName}_$model"

      // Should never happen as igluUri is already validated
      case _ => throw new IllegalArgumentException(s"Schema [$igluUri] does not conform to Iglu Schema URI regular expression")
    }
  }

  /**
   * Iglu URI of one of shred properties extracted from enriched event
   * along with actual type
   *
   * @param shredProperty type of shred property
   * @param igluUri valid Iglu URI of shred property
   */
  case class InventoryItem(shredProperty: ShredProperty, igluUri: IgluUri)

  /**
   * The event as a shredded stringified JSON along with it's inventory
   * (set of JSON fields)
   *
   * @param event stringified event JSON
   * @param inventory set of JSON fields (contexts, unsturct event)
   */
  case class EventWithInventory(event: String, inventory: Set[InventoryItem])


  /**
   * Known contexts types of enriched event
   */
  sealed trait ContextsType {
    def field: String
  }

  case object DerivedContexts extends ContextsType {
    def field = "derived_contexts"
  }

  case object CustomContexts extends ContextsType {
    def field = "contexts"
  }

  /**
   * Field types of enriched event that can be shredded (self-describing JSONs)
   */
  sealed trait ShredProperty {
    /**
     * Canonical field name
     */
    def name: String

    /**
     * Result output prefix
     */
    def prefix: String
  }

  case class Contexts(contextType: ContextsType) extends ShredProperty {
    def name = contextType.field
    def prefix = "contexts_"
  }

  case object UnstructEvent extends ShredProperty {
    def name = "unstruct_event"
    def prefix = name + "_"
  }

  /**
   * ADT representing intermediate format - output of TSV Converter functions types
   * With these data types, output can be easily separated and inspected
   */
  private[scalasdk] sealed trait TsvConverterOutput {
    /**
     * Get JSON object along with inventory
     */
    def jsonAndInventory: (Set[InventoryItem], JObject) = (getInventory, getJson)

    /**
     * Get set of contained JSON Schema URIs
     */
    def getInventory: Set[InventoryItem]

    /**
     * Get ready-to-output JSON
     */
    def getJson: JObject
  }

  /**
   * Context (either custom or derived) field, containing all data JSONs indexed by Iglu URI
   *
   * @param contextsType type of context (custom or derived)
   * @param contextMap arrays of JSONs grouped by Iglu URI
   */
  private[scalasdk] case class ContextsOutput(contextsType: ContextsType, contextMap: Map[IgluUri, List[JValue]]) extends TsvConverterOutput {
    def getInventory =
      contextMap.keySet.map { uri => InventoryItem(Contexts(contextsType), uri) }

    def getJson =
      JObject(contextMap.toList.map { case (key, list) => (fixSchema(Contexts(contextsType), key), JArray(list)) })
  }

  /**
   * unstruct_event field of enriched event, containing Iglu URI and actual data
   *
   * @param igluUri Iglu URI for data
   * @param value data
   */
  case class UnstructEventOutput(igluUri: IgluUri, value: JValue) extends TsvConverterOutput {
    def getInventory =
      Set(InventoryItem(UnstructEvent, igluUri))

    def getJson =
      JObject(fixSchema(UnstructEvent, igluUri) -> value)
  }

  /**
   * Primitive (such as app_id, gel_latitude, etc) field of enriched event.
   * Contains only key name and value (JSON because it can be bool, string, int etc)
   *
   * @param key enriched event field name
   * @param value actual JSON
   */
  private[scalasdk]  case class PrimitiveOutput(key: String, value: JValue) extends TsvConverterOutput {
    def getInventory =
      Set.empty[InventoryItem]

    def getJson =
      JObject(key -> value)
  }
}
