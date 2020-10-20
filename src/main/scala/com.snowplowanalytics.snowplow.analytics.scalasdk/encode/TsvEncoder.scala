/*
 * Copyright (c) 2020-2020 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.snowplow.analytics.scalasdk.encode

import java.time.format.DateTimeFormatter
import java.time.Instant
import java.util.UUID

import io.circe.syntax._

import com.snowplowanalytics.snowplow.analytics.scalasdk.SnowplowEvent._
import com.snowplowanalytics.snowplow.analytics.scalasdk.Event

object TsvEncoder {
  sealed trait FieldEncoder[T] {
    def encodeField(t: T): String
  }
  
  implicit object StringEncoder extends FieldEncoder[String] {
    def encodeField(str: String) = str
  }

  implicit object InstantEncoder extends FieldEncoder[Instant] {
    def encodeField(inst: Instant): String =
      DateTimeFormatter.ISO_INSTANT
        .format(inst)
        .replace("T", " ")
        .dropRight(1) // remove trailing 'Z'
  }

  implicit object UuidEncoder extends FieldEncoder[UUID] {
    def encodeField(uuid: UUID): String = uuid.toString
  }
  
  implicit object IntEncoder extends FieldEncoder[Int] {
    def encodeField(int: Int): String = int.toString
  }
  
  implicit object DoubleEncoder extends FieldEncoder[Double] {
    def encodeField(doub: Double): String = doub.toString
  }
  
  implicit object BooleanEncoder extends FieldEncoder[Boolean] {
    def encodeField(bool: Boolean): String = if(bool) "1" else "0"
  }

  implicit object ContextsEncoder extends FieldEncoder[Contexts] {
    def encodeField(ctxts: Contexts): String =
      if (ctxts.data.isEmpty)
        ""
      else
        ctxts.asJson.noSpaces
  }
  
  implicit object UnstructEncoder extends FieldEncoder[UnstructEvent] {
    def encodeField(unstruct: UnstructEvent): String =
      if (unstruct.data.isDefined)
        unstruct.asJson.noSpaces
      else
        ""
    }
  
  def encode[A](a: A)(implicit ev: FieldEncoder[A]): String =
    ev.encodeField(a)

  def encode[A](optA: Option[A])(implicit ev: FieldEncoder[A]): String =
    optA.map(a => ev.encodeField(a)).getOrElse("")
  
  def encode(event: Event): String = 
    List(
      encode(event.app_id) + "\t" +
      encode(event.platform) + "\t" +
      encode(event.etl_tstamp) + "\t" +
      encode(event.collector_tstamp) + "\t" +
      encode(event.dvce_created_tstamp) + "\t" +
      encode(event.event) + "\t" +
      encode(event.event_id) + "\t" +
      encode(event.txn_id) + "\t" +
      encode(event.name_tracker) + "\t" +
      encode(event.v_tracker) + "\t" +
      encode(event.v_collector) + "\t" +
      encode(event.v_etl) + "\t" +
      encode(event.user_id) + "\t" +
      encode(event.user_ipaddress) + "\t" +
      encode(event.user_fingerprint) + "\t" +
      encode(event.domain_userid) + "\t" +
      encode(event.domain_sessionidx) + "\t" +
      encode(event.network_userid) + "\t" +
      encode(event.geo_country) + "\t" +
      encode(event.geo_region) + "\t" +
      encode(event.geo_city) + "\t" +
      encode(event.geo_zipcode) + "\t" +
      encode(event.geo_latitude) + "\t" +
      encode(event.geo_longitude) + "\t" +
      encode(event.geo_region_name) + "\t" +
      encode(event.ip_isp) + "\t" +
      encode(event.ip_organization) + "\t" +
      encode(event.ip_domain) + "\t" +
      encode(event.ip_netspeed) + "\t" +
      encode(event.page_url) + "\t" +
      encode(event.page_title) + "\t" +
      encode(event.page_referrer) + "\t" +
      encode(event.page_urlscheme) + "\t" +
      encode(event.page_urlhost) + "\t" +
      encode(event.page_urlport) + "\t" +
      encode(event.page_urlpath) + "\t" +
      encode(event.page_urlquery) + "\t" +
      encode(event.page_urlfragment) + "\t" +
      encode(event.refr_urlscheme) + "\t" +
      encode(event.refr_urlhost) + "\t" +
      encode(event.refr_urlport) + "\t" +
      encode(event.refr_urlpath) + "\t" +
      encode(event.refr_urlquery) + "\t" +
      encode(event.refr_urlfragment) + "\t" +
      encode(event.refr_medium) + "\t" +
      encode(event.refr_source) + "\t" +
      encode(event.refr_term) + "\t" +
      encode(event.mkt_medium) + "\t" +
      encode(event.mkt_source) + "\t" +
      encode(event.mkt_term) + "\t" +
      encode(event.mkt_content) + "\t" +
      encode(event.mkt_campaign) + "\t" +
      encode(event.contexts) + "\t" +
      encode(event.se_category) + "\t" +
      encode(event.se_action) + "\t" +
      encode(event.se_label) + "\t" +
      encode(event.se_property) + "\t" +
      encode(event.se_value) + "\t" +
      encode(event.unstruct_event) + "\t" +
      encode(event.tr_orderid) + "\t" +
      encode(event.tr_affiliation) + "\t" +
      encode(event.tr_total) + "\t" +
      encode(event.tr_tax) + "\t" +
      encode(event.tr_shipping) + "\t" +
      encode(event.tr_city) + "\t" +
      encode(event.tr_state) + "\t" +
      encode(event.tr_country) + "\t" +
      encode(event.ti_orderid) + "\t" +
      encode(event.ti_sku) + "\t" +
      encode(event.ti_name) + "\t" +
      encode(event.ti_category) + "\t" +
      encode(event.ti_price) + "\t" +
      encode(event.ti_quantity) + "\t" +
      encode(event.pp_xoffset_min) + "\t" +
      encode(event.pp_xoffset_max) + "\t" +
      encode(event.pp_yoffset_min) + "\t" +
      encode(event.pp_yoffset_max) + "\t" +
      encode(event.useragent) + "\t" +
      encode(event.br_name) + "\t" +
      encode(event.br_family) + "\t" +
      encode(event.br_version) + "\t" +
      encode(event.br_type) + "\t" +
      encode(event.br_renderengine) + "\t" +
      encode(event.br_lang) + "\t" +
      encode(event.br_features_pdf) + "\t" +
      encode(event.br_features_flash) + "\t" +
      encode(event.br_features_java) + "\t" +
      encode(event.br_features_director) + "\t" +
      encode(event.br_features_quicktime) + "\t" +
      encode(event.br_features_realplayer) + "\t" +
      encode(event.br_features_windowsmedia) + "\t" +
      encode(event.br_features_gears) + "\t" +
      encode(event.br_features_silverlight) + "\t" +
      encode(event.br_cookies) + "\t" +
      encode(event.br_colordepth) + "\t" +
      encode(event.br_viewwidth) + "\t" +
      encode(event.br_viewheight) + "\t" +
      encode(event.os_name) + "\t" +
      encode(event.os_family) + "\t" +
      encode(event.os_manufacturer) + "\t" +
      encode(event.os_timezone) + "\t" +
      encode(event.dvce_type) + "\t" +
      encode(event.dvce_ismobile) + "\t" +
      encode(event.dvce_screenwidth) + "\t" +
      encode(event.dvce_screenheight) + "\t" +
      encode(event.doc_charset) + "\t" +
      encode(event.doc_width) + "\t" +
      encode(event.doc_height) + "\t" +
      encode(event.tr_currency) + "\t" +
      encode(event.tr_total_base) + "\t" +
      encode(event.tr_tax_base) + "\t" +
      encode(event.tr_shipping_base) + "\t" +
      encode(event.ti_currency) + "\t" +
      encode(event.ti_price_base) + "\t" +
      encode(event.base_currency) + "\t" +
      encode(event.geo_timezone) + "\t" +
      encode(event.mkt_clickid) + "\t" +
      encode(event.mkt_network) + "\t" +
      encode(event.etl_tags) + "\t" +
      encode(event.dvce_sent_tstamp) + "\t" +
      encode(event.refr_domain_userid) + "\t" +
      encode(event.refr_dvce_tstamp) + "\t" +
      encode(event.derived_contexts) + "\t" +
      encode(event.domain_sessionid) + "\t" +
      encode(event.derived_tstamp) + "\t" +
      encode(event.event_vendor) + "\t" +
      encode(event.event_name) + "\t" +
      encode(event.event_format) + "\t" +
      encode(event.event_version) + "\t" +
      encode(event.event_fingerprint) + "\t" +
      encode(event.true_tstamp)
    ).mkString("\t")

}