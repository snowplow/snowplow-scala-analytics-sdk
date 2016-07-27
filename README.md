# Snowplow Scala Analytics SDK

[![Build Status][travis-image]][travis]
[![Release] [release-image]][releases] 
[![License][license-image]][license]

## 1. Overview

The **[Snowplow] [snowplow]** Analytics SDK for Scala lets you work with **[Snowplow enriched events] [enriched-events]** in your Scala event processing and data modeling jobs.

Use this SDK with **[Apache Spark] [spark]**, **[AWS Lambda] [lambda]**, **[Apache Flink] [flink]**, **[Scalding] [scalding]**, **[Apache Samza] [samza]** and other Scala-compatible data processing frameworks.

## 2. Functionality

The Snowplow enriched event is a relatively complex TSV string containing self-describing JSONs. Rather than work with this structure directly in Scala, use this Analytics SDK to interact with the enriched event format:

![sdk-usage-img] [sdk-usage-img]

As the Snowplow enriched event format evolves towards a cleaner **[Apache Avro] [avro]**-based structure, we will be updating this Analytics SDK to maintain compatibility across different enriched event versions.

Currently the Analytics SDK for Scala ships with a single Event Transformer:

* The JSON Event Transformer takes a Snowplow enriched event and converts it into a JSON ready for further processing

### 2.1 JSON Event Transformer

The JSON Event Transformer is adapted from the code used to load Snowplow events into Elasticsearch in the Kinesis real-time pipeline.

It converts a Snowplow enriched event into a single JSON like so:

```json
{ "app_id":"demo","platform":"web","etl_tstamp":"2015-12-01T08:32:35.048Z",
  "collector_tstamp":"2015-12-01T04:00:54.000Z","dvce_tstamp":"2015-12-01T03:57:08.986Z",
  "event":"page_view","event_id":"f4b8dd3c-85ef-4c42-9207-11ef61b2a46e","txn_id":null,
  "name_tracker":"co","v_tracker":"js-2.5.0","v_collector":"clj-1.0.0-tom-0.2.0",...
```

The most complex piece of processing is the handling of the self-describing JSONs found in the enriched event's `unstruct_event`, `contexts` and `derived_contexts` fields. All self-describing JSONs found in the event are flattened into top-level plain (i.e. not self-describing) objects within the enriched event JSON.

For example, if an enriched event contained a `com.snowplowanalytics.snowplow/link_click/jsonschema/1-0-1`, then the final JSON would contain:

```json
{ "app_id":"demo","platform":"web","etl_tstamp":"2015-12-01T08:32:35.048Z",
  "unstruct_event_com_snowplowanalytics_snowplow_link_click_1": {
    "targetUrl":"http://www.example.com",
    "elementClasses":["foreground"],
    "elementId":"exampleLink"
  },...
```

## 3. Usage

### 3.1 Installation

The latest version of Snowplow Scala Analytics SDK is 0.1.0, which is cross-built against Scala 2.10.x and 2.11.x.

If you're using SBT, add the following lines to your build file:

```scala
// Resolvers
val snowplowRepo = "Snowplow Analytics" at "http://maven.snplow.com/releases/"

// Dependency
val analyticsSdk = "com.snowplowanalytics" %% "snowplow-scala-analytics-sdk" % "0.1.0"
```

Note the double percent (`%%`) between the group and artifactId. This will ensure that you get the right package for your Scala version.

### 3.2 Using from Apache Spark

The Scala Analytics SDK is a great fit for performing Snowplow **[event data modeling] [event-data-modeling]** in Apache Spark and Spark Streaming.

Here's the code we use internally for our own data modeling jobs:

```scala
import com.snowplowanalytics.snowplow.analytics.scalasdk.json.EventTransformer

val events = input
  .map(line => EventTransformer.transform(line))
  .filter(_.isSuccess)
  .flatMap(_.toOption)

val dataframe = ctx.read.json(events)
```

### 3.3 Using from AWS Lambda

The Scala Analytics SDK is a great fit for performing **analytics-on-write** on Snowplow event streams using AWS Lambda.

Here's some sample code for transforming enriched events into JSON inside a Scala Lambda:

```scala
import com.snowplowanalytics.snowplow.analytics.scalasdk.json.EventTransformer

def recordHandler(event: KinesisEvent) {

  val events = for {
    rec <- event.getRecords
    line = new String(rec.getKinesis.getData.array())
    json = EventTransformer.transform(line)
  } yield json
```

## 4. For contributors

Assuming git, **[Vagrant] [vagrant-install]** and **[VirtualBox] [virtualbox-install]** installed:

```bash
 host$ git clone https://github.com/snowplow/snowplow-scala-analytics-sdk.git
 host$ cd snowplow-scala-analytics-sdk
 host$ vagrant up && vagrant ssh
guest$ cd /vagrant
guest$ sbt test
```

## 5. Copyright and license

The Snowplow Scala Analytics SDK is copyright 2016 Snowplow Analytics Ltd.

Licensed under the **[Apache License, Version 2.0] [license]** (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[travis-image]: https://travis-ci.org/snowplow/snowplow-scala-analytics-sdk.png?branch=master
[travis]: http://travis-ci.org/snowplow/snowplow-scala-analytics-sdk

[license-image]: http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[license]: http://www.apache.org/licenses/LICENSE-2.0

[release-image]: http://img.shields.io/badge/release-0.1.1-blue.svg?style=flat
[releases]: https://github.com/snowplow/snowplow-scala-analytics-sdk/releases

[sdk-usage-img]: https://raw.githubusercontent.com/snowplow/snowplow-scala-analytics-sdk/master/sdk-usage.png

[snowplow]: http://snowplowanalytics.com
[enriched-events]: https://github.com/snowplow/snowplow/wiki/canonical-event-model
[event-data-modeling]: http://snowplowanalytics.com/blog/2016/03/16/introduction-to-event-data-modeling/

[spark]: http://spark.apache.org/
[lambda]: https://aws.amazon.com/lambda/
[flink]: https://flink.apache.org/
[scalding]: https://github.com/twitter/scalding
[samza]: http://samza.apache.org/
[avro]: https://avro.apache.org/

[vagrant-install]: http://docs.vagrantup.com/v2/installation/index.html
[virtualbox-install]: https://www.virtualbox.org/wiki/Downloads