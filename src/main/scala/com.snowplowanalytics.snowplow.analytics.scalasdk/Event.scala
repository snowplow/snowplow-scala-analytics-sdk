/*
 * Copyright (c) 2016-2020 Snowplow Analytics Ltd. All rights reserved.
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
import java.time.format.DateTimeFormatter

// circe
import io.circe.{Decoder, Encoder, Json, JsonObject}
import io.circe.Json.JString
import io.circe.generic.semiauto._
import io.circe.syntax._

// iglu
import com.snowplowanalytics.iglu.core.SelfDescribingData
import com.snowplowanalytics.iglu.core.circe.implicits._

// This library
import com.snowplowanalytics.snowplow.analytics.scalasdk.decode.{DecodeResult, Parser}
import com.snowplowanalytics.snowplow.analytics.scalasdk.SnowplowEvent.{Contexts, UnstructEvent}
import com.snowplowanalytics.snowplow.analytics.scalasdk.SnowplowEvent._
import com.snowplowanalytics.snowplow.analytics.scalasdk.encode.TsvEncoder

/**
 * Case class representing a canonical Snowplow event.
 *
 * @see https://docs.snowplowanalytics.com/docs/understanding-your-pipeline/canonical-event/
 */
// format: off
case class Event(
  app_id:                   Option[String],
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
  true_tstamp:              Option[Instant]
) {
  // format: on

  /**
   * Extracts metadata from the event containing information about the types and Iglu URIs of its shred properties
   */
  def inventory: Set[Data.ShreddedType] = {
    val unstructEvent = unstruct_event.data.toSet
      .map((ue: SelfDescribingData[Json]) => Data.ShreddedType(Data.UnstructEvent, ue.schema))

    val derivedContexts = derived_contexts.data.toSet
      .map((ctx: SelfDescribingData[Json]) => Data.ShreddedType(Data.Contexts(Data.DerivedContexts), ctx.schema))

    val customContexts = contexts.data.toSet
      .map((ctx: SelfDescribingData[Json]) => Data.ShreddedType(Data.Contexts(Data.CustomContexts), ctx.schema))

    customContexts ++ derivedContexts ++ unstructEvent
  }

  /**
   * Returns the event as a map of keys to Circe JSON values, while dropping inventory fields
   */
  def atomic: Map[String, Json] = jsonMap - "contexts" - "unstruct_event" - "derived_contexts"

  /**
   * Returns the event as a list of key/Circe JSON value pairs.
   * Unlike `jsonMap` and `atomic`, these keys use the ordering of the canonical event model
   */
  def ordered: List[(String, Option[Json])] =
    Event.parser.knownKeys.map(key => (key.name, jsonMap.get(key.name)))

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
    if (lossy)
      JsonObject
        .fromMap(
          atomic ++ contexts.toShreddedJson.toMap ++ derived_contexts.toShreddedJson.toMap ++ unstruct_event.toShreddedJson.toMap ++ geoLocation
        )
        .asJson
    else
      this.asJson

  /** Create the TSV representation of this event. */
  def toTsv: String = TsvEncoder.encode(this)

  /**
   * This event as a map of keys to Circe JSON values
   */
  private lazy val jsonMap: Map[String, Json] = this.asJsonObject.toMap
}

object Event {

  object unsafe {
    implicit def unsafeEventDecoder: Decoder[Event] = deriveDecoder[Event]
  }

  val FIELD_SIZES: Map[String, Int] = Map(
    "app_id" -> 255,
    "platform" -> 255,
    "event" -> 128,
    "event_id" -> 36,
    "name_tracker" -> 128,
    "v_tracker" -> 100,
    "v_collector" -> 100,
    "v_etl" -> 100,
    "user_id" -> 255,
    "user_ipaddress" -> 128,
    "user_fingerprint" -> 128,
    "domain_userid" -> 128,
    "network_userid" -> 128,
    "geo_country" -> 2,
    "geo_region" -> 3,
    "geo_city" -> 75,
    "geo_zipcode" -> 15,
    "geo_region_name" -> 100,
    "ip_isp" -> 100,
    "ip_organization" -> 128,
    "ip_domain" -> 128,
    "ip_netspeed" -> 100,
    "page_url" -> 4096,
    "page_title" -> 2000,
    "page_referrer" -> 4096,
    "page_urlscheme" -> 16,
    "page_urlhost" -> 255,
    "page_urlpath" -> 3000,
    "page_urlquery" -> 6000,
    "page_urlfragment" -> 3000,
    "refr_urlscheme" -> 16,
    "refr_urlhost" -> 255,
    "refr_urlpath" -> 6000,
    "refr_urlquery" -> 6000,
    "refr_urlfragment" -> 3000,
    "refr_medium" -> 25,
    "refr_source" -> 50,
    "refr_term" -> 255,
    "mkt_medium" -> 255,
    "mkt_source" -> 255,
    "mkt_term" -> 255,
    "mkt_content" -> 500,
    "mkt_campaign" -> 255,
    "se_category" -> 1000,
    "se_action" -> 1000,
    "se_label" -> 4096,
    "se_property" -> 1000,
    "tr_orderid" -> 255,
    "tr_affiliation" -> 255,
    "tr_city" -> 255,
    "tr_state" -> 255,
    "tr_country" -> 255,
    "ti_orderid" -> 255,
    "ti_sku" -> 255,
    "ti_name" -> 255,
    "ti_category" -> 255,
    "useragent" -> 1000,
    "br_name" -> 50,
    "br_family" -> 50,
    "br_version" -> 50,
    "br_type" -> 50,
    "br_renderengine" -> 50,
    "br_lang" -> 255,
    "br_colordepth" -> 12,
    "os_name" -> 50,
    "os_family" -> 50,
    "os_manufacturer" -> 50,
    "os_timezone" -> 255,
    "dvce_type" -> 50,
    "doc_charset" -> 128,
    "tr_currency" -> 3,
    "ti_currency" -> 3,
    "base_currency" -> 3,
    "geo_timezone" -> 64,
    "mkt_clickid" -> 128,
    "mkt_network" -> 64,
    "etl_tags" -> 500,
    "refr_domain_userid" -> 128,
    "domain_sessionid" -> 128,
    "event_vendor" -> 1000,
    "event_name" -> 1000,
    "event_format" -> 128,
    "event_version" -> 128,
    "event_fingerprint" -> 128
  )

  private def validateStr(
    k: String,
    value: String
  ): List[String] =
    if (value.length > FIELD_SIZES.getOrElse(k, Int.MaxValue))
      List(s"Field $k longer than maximum allowed size ${FIELD_SIZES.getOrElse(k, Int.MaxValue)}")
    else
      List.empty[String]

  private def validateStr(
    k: String,
    v: Option[String]
  ): List[String] =
    v match {
      case Some(value) => validateStr(k, value)
      case None => List.empty[String]
    }

  private def validator(e: Event): List[String] =
    validateStr("app_id", e.app_id) ++
      validateStr("platform", e.platform) ++
      validateStr("event", e.event) ++
      validateStr("name_tracker", e.name_tracker) ++
      validateStr("v_tracker", e.v_tracker) ++
      validateStr("v_collector", e.v_collector) ++
      validateStr("v_etl", e.v_etl) ++
      validateStr("user_id", e.user_id) ++
      validateStr("user_ipaddress", e.user_ipaddress) ++
      validateStr("user_fingerprint", e.user_fingerprint) ++
      validateStr("domain_userid", e.domain_userid) ++
      validateStr("network_userid", e.network_userid) ++
      validateStr("geo_country", e.geo_country) ++
      validateStr("geo_region", e.geo_region) ++
      validateStr("geo_city", e.geo_city) ++
      validateStr("geo_zipcode", e.geo_zipcode) ++
      validateStr("geo_region_name", e.geo_region_name) ++
      validateStr("ip_isp", e.ip_isp) ++
      validateStr("ip_organization", e.ip_organization) ++
      validateStr("ip_domain", e.ip_domain) ++
      validateStr("ip_netspeed", e.ip_netspeed) ++
      validateStr("page_url", e.page_url) ++
      validateStr("page_title", e.page_title) ++
      validateStr("page_referrer", e.page_referrer) ++
      validateStr("page_urlscheme", e.page_urlscheme) ++
      validateStr("page_urlhost", e.page_urlhost) ++
      validateStr("page_urlpath", e.page_urlpath) ++
      validateStr("page_urlquery", e.page_urlquery) ++
      validateStr("page_urlfragment", e.page_urlfragment) ++
      validateStr("refr_urlscheme", e.refr_urlscheme) ++
      validateStr("refr_urlhost", e.refr_urlhost) ++
      validateStr("refr_urlpath", e.refr_urlpath) ++
      validateStr("refr_urlquery", e.refr_urlquery) ++
      validateStr("refr_urlfragment", e.refr_urlfragment) ++
      validateStr("refr_medium", e.refr_medium) ++
      validateStr("refr_source", e.refr_source) ++
      validateStr("refr_term", e.refr_term) ++
      validateStr("mkt_medium", e.mkt_medium) ++
      validateStr("mkt_source", e.mkt_source) ++
      validateStr("mkt_term", e.mkt_term) ++
      validateStr("mkt_content", e.mkt_content) ++
      validateStr("mkt_campaign", e.mkt_campaign) ++
      validateStr("se_category", e.se_category) ++
      validateStr("se_action", e.se_action) ++
      validateStr("se_label", e.se_label) ++
      validateStr("se_property", e.se_property) ++
      validateStr("tr_orderid", e.tr_orderid) ++
      validateStr("tr_affiliation", e.tr_affiliation) ++
      validateStr("tr_city", e.tr_city) ++
      validateStr("tr_state", e.tr_state) ++
      validateStr("tr_country", e.tr_country) ++
      validateStr("ti_orderid", e.ti_orderid) ++
      validateStr("ti_sku", e.ti_sku) ++
      validateStr("ti_name", e.ti_name) ++
      validateStr("ti_category", e.ti_category) ++
      validateStr("useragent", e.useragent) ++
      validateStr("br_name", e.br_name) ++
      validateStr("br_family", e.br_family) ++
      validateStr("br_version", e.br_version) ++
      validateStr("br_type", e.br_type) ++
      validateStr("br_renderengine", e.br_renderengine) ++
      validateStr("br_lang", e.br_lang) ++
      validateStr("br_colordepth", e.br_colordepth) ++
      validateStr("os_name", e.os_name) ++
      validateStr("os_family", e.os_family) ++
      validateStr("os_manufacturer", e.os_manufacturer) ++
      validateStr("os_timezone", e.os_timezone) ++
      validateStr("dvce_type", e.dvce_type) ++
      validateStr("doc_charset", e.doc_charset) ++
      validateStr("tr_currency", e.tr_currency) ++
      validateStr("ti_currency", e.ti_currency) ++
      validateStr("base_currency", e.base_currency) ++
      validateStr("geo_timezone", e.geo_timezone) ++
      validateStr("mkt_clickid", e.mkt_clickid) ++
      validateStr("mkt_network", e.mkt_network) ++
      validateStr("etl_tags", e.etl_tags) ++
      validateStr("refr_domain_userid", e.refr_domain_userid) ++
      validateStr("domain_sessionid", e.domain_sessionid) ++
      validateStr("event_vendor", e.event_vendor) ++
      validateStr("event_name", e.event_name) ++
      validateStr("event_format", e.event_format) ++
      validateStr("event_version", e.event_version) ++
      validateStr("event_fingerprint", e.event_fingerprint)

  /**
   * Automatically derived Circe encoder
   */
  implicit val jsonEncoder: Encoder.AsObject[Event] = deriveEncoder[Event]

  implicit def eventDecoder: Decoder[Event] = unsafe.unsafeEventDecoder.ensure(validator)

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
  def minimal(
    id: UUID,
    collectorTstamp: Instant,
    vCollector: String,
    vEtl: String
  ): Event =
    Event(
      None,
      None,
      None,
      collectorTstamp,
      None,
      None,
      id,
      None,
      None,
      None,
      vCollector,
      vEtl,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      Contexts(Nil),
      None,
      None,
      None,
      None,
      None,
      UnstructEvent(None),
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      Contexts(Nil),
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None
    )
}
