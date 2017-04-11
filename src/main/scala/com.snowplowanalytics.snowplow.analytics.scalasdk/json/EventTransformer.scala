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

// Jackson
import com.fasterxml.jackson.core.JsonParseException

/**
 * Object to xxx
 */
object EventTransformer {

  private val StringField: TsvToJsonConverter = (key, value) => Right(JObject(key -> JString(value)))
  private val IntField: TsvToJsonConverter = (key, value) => Right(JObject(key -> JInt(value.toInt)))
  private val BoolField: TsvToJsonConverter = handleBooleanField
  private val DoubleField: TsvToJsonConverter = (key, value) => Right(JObject(key -> JDouble(value.toDouble)))
  private val TstampField: TsvToJsonConverter = (key, value) => Right(JObject(key -> JString(reformatTstamp(value))))
  private val ContextsField: TsvToJsonConverter = (_, value) => JsonShredder.parseContexts(value)
  private val UnstructField: TsvToJsonConverter = (_, value) => JsonShredder.parseUnstruct(value)

  private val Fields = List(
    "app_id" -> StringField,
    "platform" -> StringField,
    "etl_tstamp" -> TstampField,
    "collector_tstamp" -> TstampField,
    "dvce_created_tstamp" -> TstampField,
    "event" -> StringField,
    "event_id" -> StringField,
    "txn_id" -> IntField,
    "name_tracker" -> StringField,
    "v_tracker" -> StringField,
    "v_collector" -> StringField,
    "v_etl" -> StringField,
    "user_id" -> StringField,
    "user_ipaddress" -> StringField,
    "user_fingerprint" -> StringField,
    "domain_userid" -> StringField,
    "domain_sessionidx" -> IntField,
    "network_userid" -> StringField,
    "geo_country" -> StringField,
    "geo_region" -> StringField,
    "geo_city" -> StringField,
    "geo_zipcode" -> StringField,
    "geo_latitude" -> DoubleField,
    "geo_longitude" -> DoubleField,
    "geo_region_name" -> StringField,
    "ip_isp" -> StringField,
    "ip_organization" -> StringField,
    "ip_domain" -> StringField,
    "ip_netspeed" -> StringField,
    "page_url" -> StringField,
    "page_title" -> StringField,
    "page_referrer" -> StringField,
    "page_urlscheme" -> StringField,
    "page_urlhost" -> StringField,
    "page_urlport" -> IntField,
    "page_urlpath" -> StringField,
    "page_urlquery" -> StringField,
    "page_urlfragment" -> StringField,
    "refr_urlscheme" -> StringField,
    "refr_urlhost" -> StringField,
    "refr_urlport" -> IntField,
    "refr_urlpath" -> StringField,
    "refr_urlquery" -> StringField,
    "refr_urlfragment" -> StringField,
    "refr_medium" -> StringField,
    "refr_source" -> StringField,
    "refr_term" -> StringField,
    "mkt_medium" -> StringField,
    "mkt_source" -> StringField,
    "mkt_term" -> StringField,
    "mkt_content" -> StringField,
    "mkt_campaign" -> StringField,
    "contexts" -> ContextsField,
    "se_category" -> StringField,
    "se_action" -> StringField,
    "se_label" -> StringField,
    "se_property" -> StringField,
    "se_value" -> StringField,
    "unstruct_event" -> UnstructField,
    "tr_orderid" -> StringField,
    "tr_affiliation" -> StringField,
    "tr_total" -> DoubleField,
    "tr_tax" -> DoubleField,
    "tr_shipping" -> DoubleField,
    "tr_city" -> StringField,
    "tr_state" -> StringField,
    "tr_country" -> StringField,
    "ti_orderid" -> StringField,
    "ti_sku" -> StringField,
    "ti_name" -> StringField,
    "ti_category" -> StringField,
    "ti_price" -> DoubleField,
    "ti_quantity" -> IntField,
    "pp_xoffset_min" -> IntField,
    "pp_xoffset_max" -> IntField,
    "pp_yoffset_min" -> IntField,
    "pp_yoffset_max" -> IntField,
    "useragent" -> StringField,
    "br_name" -> StringField,
    "br_family" -> StringField,
    "br_version" -> StringField,
    "br_type" -> StringField,
    "br_renderengine" -> StringField,
    "br_lang" -> StringField,
    "br_features_pdf" -> BoolField,
    "br_features_flash" -> BoolField,
    "br_features_java" -> BoolField,
    "br_features_director" -> BoolField,
    "br_features_quicktime" -> BoolField,
    "br_features_realplayer" -> BoolField,
    "br_features_windowsmedia" -> BoolField,
    "br_features_gears" -> BoolField,
    "br_features_silverlight" -> BoolField,
    "br_cookies" -> BoolField,
    "br_colordepth" -> StringField,
    "br_viewwidth" -> IntField,
    "br_viewheight" -> IntField,
    "os_name" -> StringField,
    "os_family" -> StringField,
    "os_manufacturer" -> StringField,
    "os_timezone" -> StringField,
    "dvce_type" -> StringField,
    "dvce_ismobile" -> BoolField,
    "dvce_screenwidth" -> IntField,
    "dvce_screenheight" -> IntField,
    "doc_charset" -> StringField,
    "doc_width" -> IntField,
    "doc_height" -> IntField,
    "tr_currency" -> StringField,
    "tr_total_base" -> DoubleField,
    "tr_tax_base" -> DoubleField,
    "tr_shipping_base" -> DoubleField,
    "ti_currency" -> StringField,
    "ti_price_base" -> DoubleField,
    "base_currency" -> StringField,
    "geo_timezone" -> StringField,
    "mkt_clickid" -> StringField,
    "mkt_network" -> StringField,
    "etl_tags" -> StringField,
    "dvce_sent_tstamp" -> TstampField,
    "refr_domain_userid" -> StringField,
    "refr_device_tstamp" -> TstampField,
    "derived_contexts" -> ContextsField,
    "domain_sessionid" -> StringField,
    "derived_tstamp" -> TstampField,
    "event_vendor" -> StringField,
    "event_name" -> StringField,
    "event_format" -> StringField,
    "event_version" -> StringField,
    "event_fingerprint" -> StringField,
    "true_tstamp" -> TstampField
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
  private def converter(fieldInformation: ((String, TsvToJsonConverter), String)): Either[List[String], JObject] = {
    val ((fieldName, fieldConversionFunction), fieldValue) = fieldInformation
    if (fieldValue.isEmpty) {
      Right(JObject(fieldName -> JNull))
    } else {
      try {
        fieldConversionFunction(fieldName, fieldValue)
      } catch {
        case e @ (_ : IllegalArgumentException | _: JsonParseException) =>
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
  private def handleBooleanField(key: String, value: String): Either[List[String], JObject] =
    value match {
      case "1" => Right(JObject(key -> JBool(true)))
      case "0" => Right(JObject(key -> JBool(false)))
      case _   => Left(List("Value [%s] is not valid for field [%s]: expected 0 or 1".format(value, key)))
    }

  /**
   * Converts an aray of field values to a JSON whose keys are the field names
   *
   * @param event Array of values for the event
   * @return ValidatedRecord containing JSON for the event and the event_id (if it exists)
   */
  private def jsonifyGoodEvent(event: Array[String]): ValidatedEvent = {

    // TODO: this will be removed when this function takes an enriched event POJO instead
    if (event.length != Fields.size) {
      Left(List(s"Expected ${Fields.size} fields, received ${event.length} fields. This may be caused by attempting to use this SDK version on an older or newer version of Snowplow enriched events."))
    } else {

      val geoLocation: JObject = {
        val latitude = event(GeopointIndexes.latitude)
        val longitude = event(GeopointIndexes.longitude)
        if (latitude.nonEmpty && longitude.nonEmpty) {
          JObject("geo_location" -> JString(s"$latitude,$longitude"))
        } else {
          JObject()
        }
      }
      val validatedJObjects: List[Either[List[String], JObject]] = Fields.zip(event.toList).map(converter)
      val switched: Either[List[String], List[JObject]] = validatedJObjects.traverseEitherL
      switched.map( x => {
        val j = x.fold(geoLocation)((x, y) => y ~ x)
        compact(j)
      })
    }
  }

  /**
   * Convert an Amazon Kinesis record to a JSON string
   *
   * @param line enriched event TSV line
   * @return ValidatedRecord for the event
   */
  def transform(line: String): ValidatedEvent = {
    // The -1 is necessary to prevent trailing empty strings from being discarded
    jsonifyGoodEvent(line.split("\t", -1))
  }
}
