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

import cats.effect.concurrent.Ref
import cats.effect.{Blocker, IO}
import cats.effect.testing.specs2.CatsIO
import fs2.{Chunk, Stream}
import fs2.io.file.{createDirectory, writeAll}
import org.scalacheck.{Arbitrary, Gen}

import java.nio.file.{Files, Path, Paths}
import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.util.Random

object EnrichedEventGen extends CatsIO {
  import SnowplowEvent._

  // format: off
  private def emptyEvent(id: UUID, collectorTstamp: Instant, vCollector: String, vEtl: String): Event =
    Event(None, None, None, collectorTstamp, None, None, id, None, None, None, vCollector, vEtl, None, None, None,
      None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None,
      None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None,
      Contexts(Nil), None, None, None, None, None, UnstructEvent(None), None, None, None, None, None, None, None, None,
      None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None,
      None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None,
      None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None,
      Contexts(Nil), None, None, None, None, None, None, None, None)
  // format: on

  private val MaxTimestamp = 2871824840360L
  private val VCollector = "ssc-2.2.1-pubsub"
  private val VEtl = "beam-enrich-1.2.0-common-1.1.0"
  private val AppId = "spirit-walk"
  private val Platform = "web"
  private val NameTracker = "datacap"
  private val VTracker = "js-2.5.3-m1"
  private val UserId = "ada.blackjack@iglu.com"
  private val RefrUrlDomain = "google"
  private val RefrMedium = "search"
  private val RefrSource = "Google"
  private val MktMedium = "email"
  private val MktSource = "openemail"
  private val MktCampaign = "igloosforall"
  private val Useragent =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/601.1.56 (KHTML, like Gecko) Version/9.0 Safari/601.1.56"
  private val EventFormat = "jsonschema"
  private val EventVersion = "1-0-0"

  implicit val instantArbitrary: Arbitrary[Instant] =
    Arbitrary {
      for {
        seconds <- Gen.chooseNum(0L, MaxTimestamp)
        nanos <- Gen.chooseNum(Instant.MIN.getNano, Instant.MAX.getNano)
      } yield Instant.ofEpochMilli(seconds).plusNanos(nanos.toLong)
    }

  private val instantGen: Gen[Instant] = Arbitrary.arbitrary[Instant]

  private val ipv4Address: Gen[String] =
    for {
      a <- Gen.chooseNum(0, 255)
      b <- Gen.chooseNum(0, 255)
      c <- Gen.chooseNum(0, 255)
      d <- Gen.chooseNum(0, 255)
    } yield s"$a.$b.$c.$d"

  private val ipv6Address: Gen[String] =
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

  private val ipAddress: Gen[String] = Gen.oneOf(ipv4Address, ipv6Address)

  private val urlSchemeGen: Gen[String] = Gen.oneOf("http", "https")
  private val urlPrefixGen: Gen[String] = Gen.oneOf("", "www.")
  private val urlDomainGen: Gen[String] = strGen(7, Gen.alphaNumChar)
  private val urlTldGen: Gen[String] = Gen.oneOf(".com", ".net", ".co.uk", ".bg", ".ru", ".fr", ".tr")
  private val urlPathGen: Gen[String] = strGen(15, Gen.alphaNumChar)

  private val pageTitleGen: Gen[String] = for {
    one <- strGen(1, Gen.alphaNumChar)
    two <- strGen(2, Gen.alphaNumChar)
    three <- strGen(3, Gen.alphaNumChar)
    four <- strGen(4, Gen.alphaNumChar)
    five <- strGen(5, Gen.alphaNumChar)
  } yield Random.shuffle(List(one, two, three, four, five)).mkString(" ")

  private def strGen(n: Int, gen: Gen[Char]): Gen[String] =
    Gen.chooseNum(1, n).flatMap(len => Gen.listOfN(len, gen).map(_.mkString))

  val pageView: Gen[Event] = for {
    id <- Gen.uuid
    collectorTstamp <- instantGen
    etlTstamp <- instantGen
    userIpAddress <- ipAddress
    domainUserId <- Gen.uuid
    domainSessionIdx <- Gen.chooseNum(1, 10000)
    networkUserId <- Gen.uuid
    pageUrlScheme <- urlSchemeGen
    pageUrlPrefix <- urlPrefixGen
    pageUrlDomain <- urlDomainGen
    pageUrlTld <- urlTldGen
    pageUrlHost = s"$pageUrlPrefix$pageUrlDomain$pageUrlTld"
    pageUrlPath <- urlPathGen
    pageUrl = s"$pageUrlScheme://$pageUrlHost/$pageUrlPath"
    pageTitle <- pageTitleGen
    refrUrlScheme <- urlSchemeGen
    refrUrlPrefix <- urlPrefixGen
    refrUrlTld <- urlTldGen
    refrUrlHost = s"$refrUrlPrefix$RefrUrlDomain$refrUrlTld"
    refrUrlPath <- urlPathGen
    pageReferrer = s"$refrUrlScheme://$refrUrlHost/$refrUrlPath"
    domainSessionId <- Gen.uuid
    derivedTstamp <- instantGen
  } yield emptyEvent(id, collectorTstamp, VCollector, VEtl).copy(
    app_id = Some(AppId),
    platform = Some(Platform),
    etl_tstamp = Some(etlTstamp),
    event = Some("page_view"),
    name_tracker = Some(NameTracker),
    v_tracker = Some(VTracker),
    user_id = Some(UserId),
    user_ipaddress = Some(userIpAddress),
    domain_userid = Some(domainUserId.toString),
    domain_sessionidx = Some(domainSessionIdx),
    network_userid = Some(networkUserId.toString),
    page_url = Some(pageUrl),
    page_title = Some(pageTitle),
    page_referrer = Some(pageReferrer),
    page_urlscheme = Some(pageUrlScheme),
    page_urlhost = Some(pageUrlHost),
    page_urlpath = Some(pageUrlPath),
    refr_urlscheme = Some(refrUrlScheme),
    refr_urlhost = Some(refrUrlHost),
    refr_urlpath = Some(refrUrlPath),
    refr_medium = Some(RefrMedium),
    refr_source = Some(RefrSource),
    mkt_medium = Some(MktMedium),
    mkt_source = Some(MktSource),
    mkt_campaign = Some(MktCampaign),
    useragent = Some(Useragent),
    domain_sessionid = Some(domainSessionId.toString),
    derived_tstamp = Some(derivedTstamp),
    event_vendor = Some("com.snowplowanalytics.snowplow"),
    event_name = Some("page_view"),
    event_format = Some(EventFormat),
    event_version = Some(EventVersion)
  )

  val eventStream: Stream[IO, Event] = Stream.repeatEval(IO(pageView.sample)).collect {
    case Some(x) => x
  }

  def write(dir: Path, cardinality: Long): IO[Unit] =
    for {
      counter <- Ref.of[IO, Int](0)
      dir <- Blocker[IO].use(b => createDirectory[IO](b, dir))
      filename = counter.updateAndGet(_ + 1).map(i => Paths.get(s"${dir.toAbsolutePath}/enriched_events.$i.tsv"))
      _ <- Blocker[IO].use { b =>
             val result =
               for {
                 eventChunk <- eventStream.take(cardinality).map(_.toTsv).intersperse("\n").groupWithin(20000, 30.seconds)
                 fileName <- Stream.eval(filename)
                 _ <- Stream.eval(IO(if (!Files.exists(fileName.getParent)) Files.createDirectories(fileName.getParent)))
                 _ <- Stream.chunk(Chunk.bytes(eventChunk.toList.mkString("").getBytes())).through(writeAll[IO](fileName, b))
               } yield ()
             result.compile.drain
           }
    } yield ()
}
