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
import java.time.Instant
import java.util.UUID

// circe
import io.circe.{Encoder, Json, JsonObject, ObjectEncoder, Decoder}
import io.circe.Json.JString
import io.circe.generic.semiauto._
import io.circe.syntax._
import io.circe.java8.time._

// iglu
import com.snowplowanalytics.iglu.core.SelfDescribingData
import com.snowplowanalytics.iglu.core.circe.instances._

// This library
import com.snowplowanalytics.snowplow.analytics.scalasdk.decode.{Parser, DecodeResult}
import com.snowplowanalytics.snowplow.analytics.scalasdk.SnowplowEvent.{Contexts, UnstructEvent}
import com.snowplowanalytics.snowplow.analytics.scalasdk.SnowplowEvent._

/**
  * Case class representing a canonical Snowplow event.
  *
  * @see https://github.com/snowplow/snowplow/wiki/canonical-event-model
  */
case class Event(app_id:                   Option[String],
                 platform:                 Option[String],
                 etl_tstamp:               Option[Instant],
                 collector_tstamp:         Instant,
                 dvce_created_tstamp:      Option[Instant],
                 event:                    Option[String],
                 event_id:                 UUID,
                 txn_id:                   Option[Int],
                 name_tracker:             Option[String],
                 v_tracker:                Option[String],
                 v_collector:              String,
                 v_etl:                    String,
                 user_id:                  Option[String],
                 user_ipaddress:           Option[String],
                 user_fingerprint:         Option[String],
                 domain_userid:            Option[String],
                 domain_sessionidx:        Option[Int],
                 network_userid:           Option[String],
                 geo_country:              Option[String],
                 geo_region:               Option[String],
                 geo_city:                 Option[String],
                 geo_zipcode:              Option[String],
                 geo_latitude:             Option[Double],
                 geo_longitude:            Option[Double],
                 geo_region_name:          Option[String],
                 ip_isp:                   Option[String],
                 ip_organization:          Option[String],
                 ip_domain:                Option[String],
                 ip_netspeed:              Option[String],
                 page_url:                 Option[String],
                 page_title:               Option[String],
                 page_referrer:            Option[String],
                 page_urlscheme:           Option[String],
                 page_urlhost:             Option[String],
                 page_urlport:             Option[Int],
                 page_urlpath:             Option[String],
                 page_urlquery:            Option[String],
                 page_urlfragment:         Option[String],
                 refr_urlscheme:           Option[String],
                 refr_urlhost:             Option[String],
                 refr_urlport:             Option[Int],
                 refr_urlpath:             Option[String],
                 refr_urlquery:            Option[String],
                 refr_urlfragment:         Option[String],
                 refr_medium:              Option[String],
                 refr_source:              Option[String],
                 refr_term:                Option[String],
                 mkt_medium:               Option[String],
                 mkt_source:               Option[String],
                 mkt_term:                 Option[String],
                 mkt_content:              Option[String],
                 mkt_campaign:             Option[String],
                 contexts:                 Contexts,
                 se_category:              Option[String],
                 se_action:                Option[String],
                 se_label:                 Option[String],
                 se_property:              Option[String],
                 se_value:                 Option[Double],
                 unstruct_event:           UnstructEvent,
                 tr_orderid:               Option[String],
                 tr_affiliation:           Option[String],
                 tr_total:                 Option[Double],
                 tr_tax:                   Option[Double],
                 tr_shipping:              Option[Double],
                 tr_city:                  Option[String],
                 tr_state:                 Option[String],
                 tr_country:               Option[String],
                 ti_orderid:               Option[String],
                 ti_sku:                   Option[String],
                 ti_name:                  Option[String],
                 ti_category:              Option[String],
                 ti_price:                 Option[Double],
                 ti_quantity:              Option[Int],
                 pp_xoffset_min:           Option[Int],
                 pp_xoffset_max:           Option[Int],
                 pp_yoffset_min:           Option[Int],
                 pp_yoffset_max:           Option[Int],
                 useragent:                Option[String],
                 br_name:                  Option[String],
                 br_family:                Option[String],
                 br_version:               Option[String],
                 br_type:                  Option[String],
                 br_renderengine:          Option[String],
                 br_lang:                  Option[String],
                 br_features_pdf:          Option[Boolean],
                 br_features_flash:        Option[Boolean],
                 br_features_java:         Option[Boolean],
                 br_features_director:     Option[Boolean],
                 br_features_quicktime:    Option[Boolean],
                 br_features_realplayer:   Option[Boolean],
                 br_features_windowsmedia: Option[Boolean],
                 br_features_gears:        Option[Boolean],
                 br_features_silverlight:  Option[Boolean],
                 br_cookies:               Option[Boolean],
                 br_colordepth:            Option[String],
                 br_viewwidth:             Option[Int],
                 br_viewheight:            Option[Int],
                 os_name:                  Option[String],
                 os_family:                Option[String],
                 os_manufacturer:          Option[String],
                 os_timezone:              Option[String],
                 dvce_type:                Option[String],
                 dvce_ismobile:            Option[Boolean],
                 dvce_screenwidth:         Option[Int],
                 dvce_screenheight:        Option[Int],
                 doc_charset:              Option[String],
                 doc_width:                Option[Int],
                 doc_height:               Option[Int],
                 tr_currency:              Option[String],
                 tr_total_base:            Option[Double],
                 tr_tax_base:              Option[Double],
                 tr_shipping_base:         Option[Double],
                 ti_currency:              Option[String],
                 ti_price_base:            Option[Double],
                 base_currency:            Option[String],
                 geo_timezone:             Option[String],
                 mkt_clickid:              Option[String],
                 mkt_network:              Option[String],
                 etl_tags:                 Option[String],
                 dvce_sent_tstamp:         Option[Instant],
                 refr_domain_userid:       Option[String],
                 refr_dvce_tstamp:         Option[Instant],
                 derived_contexts:         Contexts,
                 domain_sessionid:         Option[String],
                 derived_tstamp:           Option[Instant],
                 event_vendor:             Option[String],
                 event_name:               Option[String],
                 event_format:             Option[String],
                 event_version:            Option[String],
                 event_fingerprint:        Option[String],
                 true_tstamp:              Option[Instant]) {

  /**
    * Extracts metadata from the event containing information about the types and Iglu URIs of its shred properties
    */
  def inventory: Set[Data.ShreddedType] = {
    val unstructEvent = unstruct_event
      .data
      .toSet
      .map((ue: SelfDescribingData[Json]) => Data.ShreddedType(Data.UnstructEvent, ue.schema))

    val derivedContexts = derived_contexts
      .data
      .toSet
      .map((ctx: SelfDescribingData[Json]) => Data.ShreddedType(Data.Contexts(Data.DerivedContexts), ctx.schema))

    val customContexts = contexts
      .data
      .toSet
      .map((ctx: SelfDescribingData[Json]) => Data.ShreddedType(Data.Contexts(Data.CustomContexts), ctx.schema))

    customContexts ++ derivedContexts ++ unstructEvent
  }

  /**
    * Returns the event as a map of keys to Circe JSON values, while dropping inventory fields
    */
  def atomic: Map[String, Json] = toJsonMap - "contexts" - "unstruct_event" - "derived_contexts"

  /**
    * Returns the event as a list of key/Circe JSON value pairs.
    * Unlike `toJsonMap` and `atomic`, these keys use the ordering of the canonical event model
    */
  def ordered: List[(String, Option[Json])] =
    Event.parser.knownKeys.map(key => (key.name, toJsonMap.get(key.name)))

  /**
    * Returns a compound JSON field containing information about an event's latitude and longitude,
    * or None if one of these fields doesn't exist
    */
  def geoLocation: Option[(String, Json)] =
    for {
      lat <- geo_latitude
      lon <- geo_longitude
    } yield "geo_location" -> s"$lat,$lon".asJson

  /**
    * Transforms the event to a validated JSON whose keys are the field names corresponding to the
    * EnrichedEvent POJO of the Scala Common Enrich project. If the lossy argument is true, any
    * self-describing events in the fields (unstruct_event, contexts and derived_contexts) are returned
    * in a "shredded" format (e.g. "unstruct_event_com_acme_1_myField": "value"), otherwise a standard
    * self-describing format is used.
    *
    * @param lossy Whether unstruct_event, contexts and derived_contexts should be flattened
    */
  def toJson(lossy: Boolean): Json =
    if (lossy) {
      JsonObject.fromMap(atomic ++ contexts.toShreddedJson.toMap ++ derived_contexts.toShreddedJson.toMap ++ unstruct_event.toShreddedJson.toMap ++ geoLocation).asJson
    } else {
      this.asJson
    }

  /**
    * Returns the event as a map of keys to Circe JSON values
    */
  private def toJsonMap: Map[String, Json] = this.asJsonObject.toMap
}

object Event {
  /**
    * Automatically derived Circe encoder
    */
  implicit val jsonEncoder: ObjectEncoder[Event] = deriveEncoder[Event]

  implicit def eventDecoder: Decoder[Event] = deriveDecoder[Event]

  /**
    * Derived TSV parser for the Event class
    */
  private val parser: Parser[Event] = Parser.deriveFor[Event].get

  /**
    * Converts a string with an enriched event TSV to an Event instance,
    * or a ValidatedNel containing information about errors
    *
    * @param line Enriched event TSV line
    */
  def parse(line: String): DecodeResult[Event] =
    parser.parse(line)

  /**
    * Creates an event with only required fields.
    * All optional fields are set to [[None]].
    */
  def minimal(id: UUID, collectorTstamp: Instant, vCollector: String, vEtl: String): Event =
    Event(None, None, None, collectorTstamp, None, None, id, None, None, None, vCollector, vEtl, None, None, None,
      None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None,
      None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None,
      Contexts(Nil), None, None, None, None, None, UnstructEvent(None), None, None, None, None, None, None, None, None,
      None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None,
      None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None,
      None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None,
      Contexts(Nil), None, None, None, None, None, None, None, None)
}