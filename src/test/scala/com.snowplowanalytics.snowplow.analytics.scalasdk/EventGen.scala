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

import org.scalacheck.{Arbitrary, Gen} 

import io.circe._
import io.circe.syntax._
import io.circe.{Encoder, Decoder, HCursor, Json}
import io.circe.parser._

import java.time.Instant

object EventGen {
  import SnowplowEvent._

  def strGen(n: Int, gen: Gen[Char]): Gen[String] =
    Gen.chooseNum(1, n).flatMap(len => Gen.listOfN(len, gen).map(_.mkString))

  private val MaxTimestamp = 2871824840360L

  implicit val instantArbitrary: Arbitrary[Instant] =
    Arbitrary {
      for {
        seconds <- Gen.chooseNum(0L, MaxTimestamp)
        nanos <- Gen.chooseNum(Instant.MIN.getNano, Instant.MAX.getNano)
      } yield Instant.ofEpochMilli(seconds).plusNanos(nanos.toLong)
    }

  val instantGen: Gen[Instant] =
    Arbitrary.arbitrary[Instant]

  val ipv4Address: Gen[String] =
    for {
      a <- Gen.chooseNum(0, 255)
      b <- Gen.chooseNum(0, 255)
      c <- Gen.chooseNum(0, 255)
      d <- Gen.chooseNum(0, 255)
    } yield s"$a.$b.$c.$d"

  val ipv6Address: Gen[String] =
    for {
      a <- Arbitrary.arbitrary[Short]
      b <- Arbitrary.arbitrary[Short]
      c <- Arbitrary.arbitrary[Short]
      d <- Arbitrary.arbitrary[Short]
      e <- Arbitrary.arbitrary[Short]
      f <- Arbitrary.arbitrary[Short]
      g <- Arbitrary.arbitrary[Short]
      h <- Arbitrary.arbitrary[Short]
    } yield f"$a%x:$b%x:$c%x:$d%x:$e%x:$f%x:$g%x:$h%x"

  val ipAddress: Gen[String] =
    Gen.oneOf(ipv4Address, ipv6Address)

  val platform: Gen[String] = Gen.oneOf("web", "mob", "app")

  val eventType: Gen[String] = Gen.oneOf("page_view", "page_ping", "transaction", "unstruct")

  val kv: Gen[String] = for {
    key <- strGen(15, Gen.alphaNumChar)
    value <- strGen(30, Gen.alphaNumChar)
  } yield key + "=" + value
  val queryString: Gen[String] = Gen.nonEmptyContainerOf[List, String](kv).map(_.mkString("&"))

  val contexts: Contexts = parse(EventSpec.contextsJson)
    .flatMap(_.as[Contexts])
    .getOrElse(throw new UnsupportedOperationException("can't decode contexts"))

  val unstruct: UnstructEvent = parse(EventSpec.unstructJson)
    .flatMap(_.as[UnstructEvent])
    .getOrElse(throw new UnsupportedOperationException("can't decode unstructured event"))

  val derived_contexts: Contexts = parse(EventSpec.derivedContextsJson)
    .flatMap(_.as[Contexts])
    .getOrElse(throw new UnsupportedOperationException("can't decode derived contexts"))

  val event: Gen[Event] =
    for {
      app_id <- Gen.option(strGen(512, Gen.alphaNumChar))
      platform <- Gen.option(platform)
      etl_tstamp <- Gen.option(instantGen)
      collector_tstamp <- instantGen
      dvce_created_tstamp <- Gen.option(instantGen)
      event <- Gen.option(eventType)
      event_id <- Gen.uuid
      txn_id <- Gen.option(Gen.chooseNum(1, 10000))
      name_tracker <- Gen.option(strGen(256, Gen.alphaNumChar))
      v_tracker <- Gen.option(strGen(256, Gen.alphaNumChar))
      v_collector <- strGen(512, Gen.alphaNumChar)
      v_etl <- strGen(512, Gen.alphaNumChar)
      user_id <- Gen.option(Gen.uuid).map(_.map(_.toString()))
      user_ipaddress <- Gen.option(ipAddress)
      user_fingerprint <- Gen.option(strGen(512, Gen.alphaNumChar))
      domain_userid <- Gen.option(Gen.uuid).map(_.map(_.toString()))
      domain_sessionidx <- Gen.option(Gen.chooseNum(1, 10000))
      network_userid <- Gen.option(Gen.uuid).map(_.map(_.toString()))
      geo_country <- Gen.option(strGen(3, Gen.alphaUpperChar))
      geo_region <- Gen.option(strGen(100, Gen.alphaNumChar))
      geo_city <- Gen.option(strGen(512, Gen.alphaChar))
      geo_zipcode <- Gen.option(strGen(6, Gen.alphaNumChar))
      geo_latitude <- Gen.option(Arbitrary.arbitrary[Double])
      geo_longitude  <- Gen.option(Arbitrary.arbitrary[Double])
      geo_region_name <- Gen.option(strGen(512, Gen.alphaChar))
      ip_isp <- Gen.option(strGen(512, Gen.alphaNumChar))
      ip_organization <- Gen.option(strGen(512, Gen.alphaNumChar))
      ip_domain <- Gen.option(strGen(512, Gen.alphaNumChar))
      ip_netspeed <- Gen.option(strGen(50, Gen.alphaNumChar))
      page_url <- Gen.option(strGen(512, Gen.alphaNumChar))
      page_title <- Gen.option(strGen(512, Gen.alphaNumChar))
      page_referrer <- Gen.option(strGen(512, Gen.alphaNumChar))
      page_urlscheme <- Gen.option(Gen.oneOf("http", "https"))
      page_urlhost <- Gen.option(strGen(512, Gen.alphaNumChar))
      page_urlport <- Gen.option(Gen.chooseNum(1, 65000))
      page_urlpath <- Gen.option(strGen(512, Gen.alphaNumChar))
      page_urlquery <- Gen.option(queryString)
      page_urlfragment <- Gen.option(strGen(512, Gen.alphaNumChar))
      refr_urlscheme <- Gen.option(strGen(10, Gen.alphaNumChar))
      refr_urlhost <- Gen.option(strGen(512, Gen.alphaNumChar))
      refr_urlport <- Gen.option(Gen.chooseNum(1, 65000))
      refr_urlpath <- Gen.option(strGen(512, Gen.alphaNumChar))
      refr_urlquery <- Gen.option(strGen(512, Gen.alphaNumChar))
      refr_urlfragment <- Gen.option(strGen(512, Gen.alphaNumChar))
      refr_medium <- Gen.option(strGen(512, Gen.alphaNumChar))
      refr_source <- Gen.option(strGen(512, Gen.alphaNumChar))
      refr_term <- Gen.option(strGen(512, Gen.alphaNumChar))
      mkt_medium <- Gen.option(strGen(512, Gen.alphaNumChar))
      mkt_source <- Gen.option(strGen(512, Gen.alphaNumChar))
      mkt_term <- Gen.option(strGen(512, Gen.alphaNumChar))
      mkt_content <- Gen.option(strGen(512, Gen.alphaNumChar))
      mkt_campaign <- Gen.option(strGen(512, Gen.alphaNumChar))
      contexts <- Gen.oneOf(contexts, Contexts(Nil))
      se_category <- Gen.option(strGen(512, Gen.alphaNumChar))
      se_action <- Gen.option(strGen(512, Gen.alphaNumChar))
      se_label <- Gen.option(strGen(512, Gen.alphaNumChar))
      se_property <- Gen.option(strGen(512, Gen.alphaNumChar))
      se_value <- Gen.option(Arbitrary.arbitrary[Double])
      unstruct_event = event match {
        case Some("unstruct") => unstruct
        case _ => UnstructEvent(None)
      }
      tr_orderid <- Gen.option(Gen.uuid).map(_.map(_.toString()))
      tr_affiliation <- Gen.option(strGen(512, Gen.alphaNumChar))
      tr_total <- Gen.option(Arbitrary.arbitrary[Double])
      tr_tax <- Gen.option(Arbitrary.arbitrary[Double])
      tr_shipping <- Gen.option(Arbitrary.arbitrary[Double])
      tr_city <- Gen.option(strGen(512, Gen.alphaNumChar))
      tr_state <- Gen.option(strGen(512, Gen.alphaNumChar))
      tr_country <- Gen.option(strGen(512, Gen.alphaNumChar))
      ti_orderid <- Gen.option(Gen.uuid).map(_.map(_.toString()))
      ti_sku <- Gen.option(strGen(512, Gen.alphaNumChar))
      ti_name <- Gen.option(strGen(512, Gen.alphaNumChar))
      ti_category <- Gen.option(strGen(512, Gen.alphaNumChar))
      ti_price <- Gen.option(Arbitrary.arbitrary[Double])
      ti_quantity <- Gen.option(Gen.chooseNum(1, 100))
      pp_xoffset_min <- Gen.option(Gen.chooseNum(1, 10000))
      pp_xoffset_max <- Gen.option(Gen.chooseNum(1, 10000))
      pp_yoffset_min <- Gen.option(Gen.chooseNum(1, 10000))
      pp_yoffset_max <- Gen.option(Gen.chooseNum(1, 10000))
      useragent <- Gen.option(strGen(512, Gen.alphaNumChar))
      br_name <- Gen.option(strGen(512, Gen.alphaNumChar))
      br_family <- Gen.option(strGen(512, Gen.alphaNumChar))
      br_version <- Gen.option(strGen(512, Gen.alphaNumChar))
      br_type <- Gen.option(strGen(512, Gen.alphaNumChar))
      br_renderengine <- Gen.option(strGen(512, Gen.alphaNumChar))
      br_lang <- Gen.option(strGen(512, Gen.alphaNumChar))
      br_features_pdf <- Gen.option(Arbitrary.arbitrary[Boolean])
      br_features_flash <- Gen.option(Arbitrary.arbitrary[Boolean])
      br_features_java <- Gen.option(Arbitrary.arbitrary[Boolean])
      br_features_director <- Gen.option(Arbitrary.arbitrary[Boolean])
      br_features_quicktime <- Gen.option(Arbitrary.arbitrary[Boolean])
      br_features_realplayer <- Gen.option(Arbitrary.arbitrary[Boolean])
      br_features_windowsmedia <- Gen.option(Arbitrary.arbitrary[Boolean])
      br_features_gears <- Gen.option(Arbitrary.arbitrary[Boolean])
      br_features_silverlight <- Gen.option(Arbitrary.arbitrary[Boolean])
      br_cookies <- Gen.option(Arbitrary.arbitrary[Boolean])
      br_colordepth <- Gen.option(strGen(512, Gen.alphaNumChar))
      br_viewwidth <- Gen.option(Gen.chooseNum(1, 10000))
      br_viewheight <- Gen.option(Gen.chooseNum(1, 10000))
      os_name <- Gen.option(strGen(512, Gen.alphaNumChar))
      os_family <- Gen.option(strGen(512, Gen.alphaNumChar))
      os_manufacturer <- Gen.option(strGen(512, Gen.alphaNumChar))
      os_timezone <- Gen.option(strGen(512, Gen.alphaNumChar))
      dvce_type <- Gen.option(strGen(512, Gen.alphaNumChar))
      dvce_ismobile <- Gen.option(Arbitrary.arbitrary[Boolean])
      dvce_screenwidth <- Gen.option(Gen.chooseNum(1, 10000))
      dvce_screenheight <- Gen.option(Gen.chooseNum(1, 10000))
      doc_charset <- Gen.option(strGen(512, Gen.alphaNumChar))
      doc_width <- Gen.option(Gen.chooseNum(1, 10000))
      doc_height <- Gen.option(Gen.chooseNum(1, 10000))
      tr_currency <- Gen.option(strGen(512, Gen.alphaNumChar))
      tr_total_base <- Gen.option(Arbitrary.arbitrary[Double])
      tr_tax_base <- Gen.option(Arbitrary.arbitrary[Double])
      tr_shipping_base <- Gen.option(Arbitrary.arbitrary[Double])
      ti_currency <- Gen.option(strGen(512, Gen.alphaNumChar))
      ti_price_base <- Gen.option(Arbitrary.arbitrary[Double])
      base_currency <- Gen.option(strGen(512, Gen.alphaNumChar))
      geo_timezone <- Gen.option(strGen(512, Gen.alphaNumChar))
      mkt_clickid <- Gen.option(Gen.uuid).map(_.map(_.toString()))
      mkt_network <- Gen.option(strGen(512, Gen.alphaNumChar))
      etl_tags <- Gen.option(strGen(512, Gen.alphaNumChar))
      dvce_sent_tstamp <- Gen.option(instantGen)
      refr_domain_userid <- Gen.option(Gen.uuid).map(_.map(_.toString()))
      refr_dvce_tstamp <- Gen.option(instantGen)
      derived_contexts <- Gen.oneOf(derived_contexts, Contexts(Nil))
      domain_sessionid <- Gen.option(Gen.uuid).map(_.map(_.toString()))
      derived_tstamp  <- Gen.option(instantGen)
      event_vendor <- Gen.option(Gen.identifier)
      event_name <- Gen.option(Gen.identifier)
      event_format <- Gen.option("jsonschema")
      event_version <- Gen.option(strGen(10, Gen.alphaNumChar))
      event_fingerprint <- Gen.option(strGen(512, Gen.alphaNumChar))
      true_tstamp <- Gen.option(instantGen)
    } yield Event(
      app_id,
      platform,
      etl_tstamp,
      collector_tstamp,
      dvce_created_tstamp,
      event,
      event_id,
      txn_id,
      name_tracker,
      v_tracker,
      v_collector,
      v_etl,
      user_id,
      user_ipaddress,
      user_fingerprint,
      domain_userid,
      domain_sessionidx,
      network_userid,
      geo_country,
      geo_region,
      geo_city,
      geo_zipcode,
      geo_latitude,
      geo_longitude,
      geo_region_name,
      ip_isp,
      ip_organization,
      ip_domain,
      ip_netspeed,
      page_url,
      page_title,
      page_referrer,
      page_urlscheme,
      page_urlhost,
      page_urlport,
      page_urlpath,
      page_urlquery,
      page_urlfragment,
      refr_urlscheme,
      refr_urlhost,
      refr_urlport,
      refr_urlpath,
      refr_urlquery,
      refr_urlfragment,
      refr_medium,
      refr_source,
      refr_term,
      mkt_medium,
      mkt_source,
      mkt_term,
      mkt_content,
      mkt_campaign,
      contexts,
      se_category,
      se_action,
      se_label,
      se_property,
      se_value,
      unstruct_event,
      tr_orderid,
      tr_affiliation,
      tr_total,
      tr_tax,
      tr_shipping,
      tr_city,
      tr_state,
      tr_country,
      ti_orderid,
      ti_sku,
      ti_name,
      ti_category,
      ti_price,
      ti_quantity,
      pp_xoffset_min,
      pp_xoffset_max,
      pp_yoffset_min,
      pp_yoffset_max,
      useragent,
      br_name,
      br_family,
      br_version,
      br_type,
      br_renderengine,
      br_lang,
      br_features_pdf,
      br_features_flash,
      br_features_java,
      br_features_director,
      br_features_quicktime,
      br_features_realplayer,
      br_features_windowsmedia,
      br_features_gears,
      br_features_silverlight,
      br_cookies,
      br_colordepth,
      br_viewwidth,
      br_viewheight,
      os_name,
      os_family,
      os_manufacturer,
      os_timezone,
      dvce_type,
      dvce_ismobile,
      dvce_screenwidth,
      dvce_screenheight,
      doc_charset,
      doc_width,
      doc_height,
      tr_currency,
      tr_total_base,
      tr_tax_base,
      tr_shipping_base,
      ti_currency,
      ti_price_base,
      base_currency,
      geo_timezone,
      mkt_clickid,
      mkt_network,
      etl_tags,
      dvce_sent_tstamp,
      refr_domain_userid,
      refr_dvce_tstamp,
      derived_contexts,
      domain_sessionid,
      derived_tstamp,
      event_vendor,
      event_name,
      event_format,
      event_version,
      event_fingerprint,
      true_tstamp
    )
}
