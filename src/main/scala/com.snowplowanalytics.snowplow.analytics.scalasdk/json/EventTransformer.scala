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
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.util.parsing.json.JSONObject

// Jackson
import com.fasterxml.jackson.core.JsonParseException

// This library
import Data._

/**
 * TSV to JSON
 */
object EventTransformer {


  /**
   * Convert a string with Enriched event TSV to a JSON string
   *
   * @param line enriched event TSV line
   * @return ValidatedRecord for the event
   */
  def transform(line: String): ValidatedEvent = {
    // The -1 is necessary to prevent trailing empty strings from being discarded
    jsonifyGoodEvent(line.split("\t", -1)).map { case (_, json) => compact(json) }
  }

  /**
   * Convert an Amazon Kinesis record to a JSON string
   *
   * @param line enriched event TSV line
   * @return ValidatedRecord for the event
   */
  def transformWithInventory(line: String): ValidatedEventWithInventory = {
    jsonifyGoodEvent(line.split("\t", -1)).map { case (inventory, json) =>
      EventWithInventory(compact(json), inventory)
    }
  }

  /**
   * Converts an array of field values to a JSON whose keys are the field names
   *
   * @param event Array of values for the event
   * @return ValidatedRecord containing JSON for the event and the event_id (if it exists)
   */
  def jsonifyGoodEvent(event: Array[String]): Validated[(Set[InventoryItem], JObject)] = {
    getValidatedJsonEvent(event, true)
  }

  /**
    * Converts an array of field values to a validated JSON whose keys are the field names corresponding to the
    * EnrichedEvent POJO of the Scala Common Enrich project. If there are any self-describing events in the fields:
    * "unstruct_event", "derived_contexts" or "contexts" these are returned in a "shredded" format (e.g.
    * "unstruct_event_com_acme_1_myField": "value") when the flatten argument is true. 
    * When the flatten argument is false the nested structure is returned for those three fields instead.
    *
    * @param event Array of values for the event
    * @param flatten Whether to flatten the fields in "unstruct_event", "contexts" and "derived_contexts"
    * @return ValidatedRecord containing JSON for the event and the event_id (if it exists)
    */
  def getValidatedJsonEvent(event: Array[String], flatten: Boolean): Validated[(Set[InventoryItem], JObject)] = {
    if (isRightSize(event)) {
      Left(List(s"Expected ${Fields.size} fields, received ${event.length} fields. This may be caused by attempting to use this SDK version on an older (pre-R73) or newer version of Snowplow enriched events."))
    } else {
      val geoLocation: JObject = getGeoLocationField(event)
      if (flatten) convertEvent(event.toList, geoLocation)
      else getJObjectWithNestedStructures(event)
    }
  }

  private def getGeoLocationField(event: Array[String]): JObject = {
    val latitude = event(GeopointIndexes.latitude)
    val longitude = event(GeopointIndexes.longitude)
    if (latitude.nonEmpty && longitude.nonEmpty) {
      JObject("geo_location" -> JString(s"$latitude,$longitude"))
    } else {
      JObject()
    }
  }

  private def getJObjectWithNestedStructures(event: Array[String]): Validated[(Set[InventoryItem], JObject)] = {
    for {
      jObject <- convertTsvEventToNestedJObject(event)
    } yield (getInventory(jObject).toSet, getGeoLocationField(event) ~ jObject)
  }

  /** Extract inventory from non-flatten JSON */
  private def getInventory(nonFlattenJson: JObject) = {
    def getSchema(json: JValue): Option[IgluUri] =
      json match {
        case JObject(fields) =>
          fields.toMap.get("schema") match {
            case Some(JString(schema)) => Some(schema)
            case _ => None
          }
        case _ => None
      }

    val map = nonFlattenJson.obj.toMap

    def getContexts(ctxType: ShredProperty): List[InventoryItem] = {
      (for {
        JObject(fields) <- map.get(ctxType.name)
        JArray(contexts) <- fields.toMap.get("data")
        schemas = contexts.flatMap(ctx => getSchema(ctx).map(uri => InventoryItem(ctxType, uri)))
      } yield schemas).getOrElse(List.empty)
    }

    val unstructEvent = for {
      JObject(fields) <- map.get(UnstructEvent.name)
      event <- fields.toMap.get("data")
      schema <- getSchema(event)
    } yield InventoryItem(UnstructEvent, schema)
    val customContexts = getContexts(Contexts(CustomContexts))
    val derivedContexts = getContexts(Contexts(DerivedContexts))

    customContexts ++ derivedContexts ++ unstructEvent.toList
  }

  /**
    * Event has been checked for size
    *
    * @param event the event array
    * @return either a list of errors or the JObject representation
    */
  private def convertTsvEventToNestedJObject(event: Array[String]): Validated[JObject]= {
    val validJObjects = Fields.zip(event).map {
      case ((fieldName, fieldType), fieldValue) => fieldParser(fieldName, fieldValue, fieldType)
    }
    validJObjects.partition(_.isLeft) match {
      case (errors, _) if errors.nonEmpty => Left(for (Left(error) <- errors) yield error)
      case (_, kvs) => Right(JObject(for (Right(kv) <- kvs) yield kv))
    }
  }

  private def fieldParser(fieldName: ShredFieldName, fieldValue: String, fieldType: TsvToJsonConverter): Either[String, (String, JValue)] =
    try {
      Right(fieldName -> stringToJValue(fieldType, fieldValue))
    } catch {
      case nfe: NumberFormatException => Left(nfe.toString)
      case bfe: BooleanFormatException => Left(bfe.toString)
    }

  private def stringToJValue(fieldType: TsvToJsonConverter, fieldValue: String): JValue =
    if (isJObjectType(fieldType)) stringToJObjectConverter(fieldType, fieldValue)
    else stringToScalarJValueConverter(fieldType, fieldValue)

  private def isJObjectType(fieldType: TsvToJsonConverter): Boolean =
    fieldType == CustomContextsField || fieldType == DerivedContextsField || fieldType == UnstructField

  private def stringToJObjectConverter(fieldType: TsvToJsonConverter, fieldValue: String): JValue = {
    if(fieldValue.isEmpty) JNothing
    else if(isJObjectType(fieldType)) parse(fieldValue)
    else throw new IllegalArgumentException(s"Unknown type: $fieldType")
  }

  private def stringToScalarJValueConverter(fieldType: TsvToJsonConverter, fieldValue: String): JValue = {
    if (fieldValue.isEmpty) JNull
    else fieldType match {
      case c: TsvToJsonConverter if c == StringField                    =>  JString(fieldValue)
      case c: TsvToJsonConverter if c == IntField                       =>  JInt(fieldValue.toInt)
      case c: TsvToJsonConverter if c == DoubleField                    =>  JDouble(fieldValue.toDouble)
      case c: TsvToJsonConverter if c == TstampField                    =>  JString(reformatTstamp(fieldValue))
      case c: TsvToJsonConverter if c == BoolField && fieldValue == "1" => JBool(true)
      case c: TsvToJsonConverter if c == BoolField && fieldValue == "0" =>  JBool(false)
      case c: TsvToJsonConverter if c == BoolField                      =>  throw new BooleanFormatException(s"Invalid boolean value: $fieldValue")
      case _ =>  throw new IllegalArgumentException(s"Unknown type: $fieldType")
    }
  }

  private def isRightSize(event: Array[String]): Boolean =
    event.length != Fields.size

  private val StringField: TsvToJsonConverter          = (key, value) => Right(PrimitiveOutput(key, JString(value)))
  private val IntField: TsvToJsonConverter             = (key, value) => Right(PrimitiveOutput(key, JInt(value.toInt)))
  private val DoubleField: TsvToJsonConverter          = (key, value) => Right(PrimitiveOutput(key, JDouble(value.toDouble)))
  private val TstampField: TsvToJsonConverter          = (key, value) => Right(PrimitiveOutput(key, JString(reformatTstamp(value))))
  private val BoolField: TsvToJsonConverter            = (key, value) => handleBooleanField(key, value)
  private val CustomContextsField: TsvToJsonConverter  = (_, value)   => JsonShredder.parseContexts(CustomContexts)(value)
  private val DerivedContextsField: TsvToJsonConverter = (_, value)   => JsonShredder.parseContexts(DerivedContexts)(value)
  private val UnstructField: TsvToJsonConverter        = (_, value)   => JsonShredder.parseUnstruct(value)

  private val Fields = List(
    "app_id"                   -> StringField,
    "platform"                 -> StringField,
    "etl_tstamp"               -> TstampField,
    "collector_tstamp"         -> TstampField,
    "dvce_created_tstamp"      -> TstampField,
    "event"                    -> StringField,
    "event_id"                 -> StringField,
    "txn_id"                   -> IntField,
    "name_tracker"             -> StringField,
    "v_tracker"                -> StringField,
    "v_collector"              -> StringField,
    "v_etl"                    -> StringField,
    "user_id"                  -> StringField,
    "user_ipaddress"           -> StringField,
    "user_fingerprint"         -> StringField,
    "domain_userid"            -> StringField,
    "domain_sessionidx"        -> IntField,
    "network_userid"           -> StringField,
    "geo_country"              -> StringField,
    "geo_region"               -> StringField,
    "geo_city"                 -> StringField,
    "geo_zipcode"              -> StringField,
    "geo_latitude"             -> DoubleField,
    "geo_longitude"            -> DoubleField,
    "geo_region_name"          -> StringField,
    "ip_isp"                   -> StringField,
    "ip_organization"          -> StringField,
    "ip_domain"                -> StringField,
    "ip_netspeed"              -> StringField,
    "page_url"                 -> StringField,
    "page_title"               -> StringField,
    "page_referrer"            -> StringField,
    "page_urlscheme"           -> StringField,
    "page_urlhost"             -> StringField,
    "page_urlport"             -> IntField,
    "page_urlpath"             -> StringField,
    "page_urlquery"            -> StringField,
    "page_urlfragment"         -> StringField,
    "refr_urlscheme"           -> StringField,
    "refr_urlhost"             -> StringField,
    "refr_urlport"             -> IntField,
    "refr_urlpath"             -> StringField,
    "refr_urlquery"            -> StringField,
    "refr_urlfragment"         -> StringField,
    "refr_medium"              -> StringField,
    "refr_source"              -> StringField,
    "refr_term"                -> StringField,
    "mkt_medium"               -> StringField,
    "mkt_source"               -> StringField,
    "mkt_term"                 -> StringField,
    "mkt_content"              -> StringField,
    "mkt_campaign"             -> StringField,
    "contexts"                 -> CustomContextsField,
    "se_category"              -> StringField,
    "se_action"                -> StringField,
    "se_label"                 -> StringField,
    "se_property"              -> StringField,
    "se_value"                 -> DoubleField,
    "unstruct_event"           -> UnstructField,
    "tr_orderid"               -> StringField,
    "tr_affiliation"           -> StringField,
    "tr_total"                 -> DoubleField,
    "tr_tax"                   -> DoubleField,
    "tr_shipping"              -> DoubleField,
    "tr_city"                  -> StringField,
    "tr_state"                 -> StringField,
    "tr_country"               -> StringField,
    "ti_orderid"               -> StringField,
    "ti_sku"                   -> StringField,
    "ti_name"                  -> StringField,
    "ti_category"              -> StringField,
    "ti_price"                 -> DoubleField,
    "ti_quantity"              -> IntField,
    "pp_xoffset_min"           -> IntField,
    "pp_xoffset_max"           -> IntField,
    "pp_yoffset_min"           -> IntField,
    "pp_yoffset_max"           -> IntField,
    "useragent"                -> StringField,
    "br_name"                  -> StringField,
    "br_family"                -> StringField,
    "br_version"               -> StringField,
    "br_type"                  -> StringField,
    "br_renderengine"          -> StringField,
    "br_lang"                  -> StringField,
    "br_features_pdf"          -> BoolField,
    "br_features_flash"        -> BoolField,
    "br_features_java"         -> BoolField,
    "br_features_director"     -> BoolField,
    "br_features_quicktime"    -> BoolField,
    "br_features_realplayer"   -> BoolField,
    "br_features_windowsmedia" -> BoolField,
    "br_features_gears"        -> BoolField,
    "br_features_silverlight"  -> BoolField,
    "br_cookies"               -> BoolField,
    "br_colordepth"            -> StringField,
    "br_viewwidth"             -> IntField,
    "br_viewheight"            -> IntField,
    "os_name"                  -> StringField,
    "os_family"                -> StringField,
    "os_manufacturer"          -> StringField,
    "os_timezone"              -> StringField,
    "dvce_type"                -> StringField,
    "dvce_ismobile"            -> BoolField,
    "dvce_screenwidth"         -> IntField,
    "dvce_screenheight"        -> IntField,
    "doc_charset"              -> StringField,
    "doc_width"                -> IntField,
    "doc_height"               -> IntField,
    "tr_currency"              -> StringField,
    "tr_total_base"            -> DoubleField,
    "tr_tax_base"              -> DoubleField,
    "tr_shipping_base"         -> DoubleField,
    "ti_currency"              -> StringField,
    "ti_price_base"            -> DoubleField,
    "base_currency"            -> StringField,
    "geo_timezone"             -> StringField,
    "mkt_clickid"              -> StringField,
    "mkt_network"              -> StringField,
    "etl_tags"                 -> StringField,
    "dvce_sent_tstamp"         -> TstampField,
    "refr_domain_userid"       -> StringField,
    "refr_device_tstamp"       -> TstampField,
    "derived_contexts"         -> DerivedContextsField,
    "domain_sessionid"         -> StringField,
    "derived_tstamp"           -> TstampField,
    "event_vendor"             -> StringField,
    "event_name"               -> StringField,
    "event_format"             -> StringField,
    "event_version"            -> StringField,
    "event_fingerprint"        -> StringField,
    "true_tstamp"              -> TstampField
  )
  
  private object GeopointIndexes {
    val latitude = 22
    val longitude = 23
  }

  /**
   * Convert the value of a field to a JValue based on the name of the field
   *
   * @param fieldInformation ((field name, field-to-JObject conversion function), field value)
   * @return JObject representing a single field in the JSON
   */
  private def converter(fieldInformation: ((String, TsvToJsonConverter), String)): Validated[(Set[InventoryItem], JObject)] = {
    val ((fieldName, fieldConversionFunction), fieldValue) = fieldInformation
    if (fieldValue.isEmpty) {
      if (fieldName.startsWith("contexts") || fieldName.startsWith("unstruct_event") || fieldName.startsWith("derived_contexts")) {
        Right((Set.empty, JObject(fieldName -> JNothing)))
      } else {
        Right((Set.empty, JObject(fieldName -> JNull)))
      }
    } else {
      try {
        fieldConversionFunction(fieldName, fieldValue).map(_.jsonAndInventory)
      } catch {
        case e@(_: IllegalArgumentException | _: JsonParseException) =>
          Left(List("Value [%s] is not valid for field [%s]: %s".format(fieldValue, fieldName, e.getMessage)))
      }
    }
  }


  /**
   * Converts a timestamp to ISO 8601 format
   *
   * @param tstamp Timestamp of the form YYYY-MM-DD hh:mm:ss
   * @return ISO 8601 timestamp
   */
  private def reformatTstamp(tstamp: String): String = tstamp.replaceAll(" ", "T") + "Z"

  /**
   * Converts "0" to false and "1" to true
   *
   * @param key The field name
   * @param value The field value - should be "0" or "1"
   * @return Validated JObject
   */
  private def handleBooleanField(key: String, value: String): Validated[PrimitiveOutput] =
    value match {
      case "1" => Right(PrimitiveOutput(key, JBool(true)))
      case "0" => Right(PrimitiveOutput(key, JBool(false)))
      case _   => Left(List("Value [%s] is not valid for field [%s]: expected 0 or 1".format(value, key)))
    }

  /**
   * Apply to each field corresponding converter and merge key-value pairs list into a single JSON object
   *
   * @param eventTsv list of enriched event columns
   * @param initial initial (probably empty) JSON object
   * @return either aggregated list of converter errors or merged JSON Object
   */
  private[json] def convertEvent(eventTsv: List[String], initial: JObject): Validated[(Set[InventoryItem], JObject)] = {
    val initialPair = (Set.empty[InventoryItem], initial)

    val result = Fields.zip(eventTsv).map(x => converter(x)).traverseEitherL.map { kvPairsList =>
      kvPairsList.fold(initialPair) { case ((accumInventory, accumObject), (inventory, kvPair)) =>
        (accumInventory ++ inventory, kvPair ~ accumObject)}
    }

    result.map { case (inventory, json) => (inventory, foldContexts(json)) }
  }

  /**
    * Merge context-arrays into its own JSON-keys
    * `{"contexts_foo_1": [{"value": 1}], "contexts_foo_1": [{"value": 2}]`
    * becomes
    * `{"contexts_foo_1": [{"value": 1}, {"value": 2}]`
    *
    * NOTE: this functions has assumptions:
    * 1. `JObject` can contain multiple identical keys (valid as per Json4s)
    * 2. All keys with `contexts_` can contain only arrays
    *
    * @param eventObject almost-ready enriched event JSON
    * @return final enriched JSON
    */
  private[json] def foldContexts(eventObject: JObject): JObject = {
    val (contexts, nonContexts) = eventObject.obj.partition { case (k, _) => k.startsWith("contexts_")}

    // Traverse all found contexts and merge-in twin-contexts
    val foldedContexts = contexts.foldLeft(List.empty[(String, JValue)]) {
      case (collapsed, (currentKey, currentContexts: JArray)) =>
        // Merge context-arrays if keys are identical
        val merged = collapsed.map {
          case (contextKey, contexts: JArray) if contextKey == currentKey =>
            (currentKey, JArray(contexts.arr ++ currentContexts.arr))
          case other => other
        }

        // Make sure only one instance of particular context resides in `collapsed`
        val keys = collapsed.map(_._1)
        if (keys.contains(currentKey)) merged else (currentKey, currentContexts) :: merged

      case (collapsed, (key, value)) => (key, value) :: collapsed   // Should never happen
    }
    JObject(foldedContexts ++ nonContexts)
  }
}
