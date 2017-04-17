/*
 * Copyright (c) 2016-2017 Snowplow Analytics Ltd. All rights reserved.
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
package json

// Specs2
import org.specs2.Specification

class RunManifestsSpec extends Specification { def is = s2"""
  splitFullPath works for bucket root $e1
  splitFullPath works for bucket root with trailing slash $e2
  splitFullPath works with prefix $e3
  splitFullPath works with prefix and trailing slash $e4
  """

  def e1 =
    RunManifests.splitFullPath("s3://some-bucket") must beEqualTo(("some-bucket", None))

  def e2 =
    RunManifests.splitFullPath("s3://some-bucket/") must beEqualTo(("some-bucket", None))

  def e3 =
    RunManifests.splitFullPath("s3://some-bucket/some/prefix") must beEqualTo(("some-bucket", Some("some/prefix/")))

  def e4 =
    RunManifests.splitFullPath("s3://some-bucket/prefix/") must beEqualTo(("some-bucket", Some("prefix/")))

}
