/*
 * Copyright (c) 2012-2017 Snowplow Analytics Ltd. All rights reserved.
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

import java.util.UUID
import java.time.Instant

import scala.collection.immutable.ListMap

import org.json4s.JObject
import org.json4s.JsonAST.{JString, JValue}

import com.snowplowanalytics.iglu.core.{SchemaKey, SelfDescribingData}
import com.snowplowanalytics.snowplow.analytics.scalasdk.json.{Validated, ValidatedEvent}

import json._   // for type classes
import json.Data._
import json.EventTransformer._

case class Event(atomic: ListMap[String, String],
                 unstructEvent: Option[SelfDescribingData[JValue]],
                 contexts: List[SelfDescribingData[JValue]],
                 derivedContexts: List[SelfDescribingData[JValue]]) {
  def asJson: JObject = ???
  def asJsonString: String = ???

  def eventId: UUID = ???
  def collectorTstamp: Instant  = ???
  def vCollector: String = ???
  def vEtl: String = ???

  def types: Set[SchemaKey] = ???
}

object Event {
  val EventIdIdx = 0
  val CollectorTstampIdx = 0

  val UnstructEventIdx = 0
  val Contextsdx = 0
  val DerivedContextsIdx = 0 // ?

  val TotalFields = 131 // ?

  def parse(s: String): Either[List[String], Event] = {

    val event = s.split("\t", -1).toVector
    if (event.length != Fields.size) {
      Left(List(s"Expected ${Fields.size} fields, received ${event.length} fields. This may be caused by attempting to use this SDK version on an older (pre-R73) or newer version of Snowplow enriched events."))
    } else {
      val geoLocation = getGeolocation(event).fold(JObject()) { case (k, v) => JObject(k -> JString(v))}

      val initialPair = (Set.empty[InventoryItem], geoLocation)

      val result = Fields.zip(event).map(x => converter(x)).traverseEitherL.map { kvPairsList =>
        kvPairsList.fold(initialPair) { case ((accumInventory, accumObject), (inventory, kvPair)) => (accumInventory ++ inventory, kvPair ~ accumObject) }
      }

      result.map { case (inventory, json) => (inventory, foldContexts(json)) }
    }
  }

  private def getGeolocation(tsv: Vector[String]): Option[(String, String)] = {
    val latitude = tsv(GeopointIndexes.latitude)
    val longitude = tsv(GeopointIndexes.longitude)
    if (latitude.nonEmpty && longitude.nonEmpty) {
      Some("geo_location" -> s"$latitude,$longitude")
    } else {
      None
    }
  }
}
