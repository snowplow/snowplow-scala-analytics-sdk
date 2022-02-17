package com.snowplowanalytics.snowplow.analytics.scalasdk

package object validate {

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

  def validator(e: Event): List[String] =
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

}
