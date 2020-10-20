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
package com.snowplowanalytics.snowplow.analytics.scalasdk.benchmark

import org.openjdk.jmh.annotations._

import java.util.concurrent.TimeUnit
import java.util.UUID
import java.time.Instant

import com.snowplowanalytics.snowplow.analytics.scalasdk.Event

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime, Mode.Throughput))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class OrderedBenchmark {
  @Benchmark
  def ordered(state : States.AtomicEventState): Unit = {
    state.event.ordered
  }
}

object States {
  @State(Scope.Benchmark)
  class AtomicEventState {
    var event: Event = _

    @Setup(Level.Trial)
    def init(): Unit = {
      val uuid = UUID.randomUUID()
      val timestamp = Instant.now()
      val vCollector = "2.0.0"
      val vTracker = "scala_0.7.0"
      event = Event.minimal(uuid, timestamp, vCollector, vTracker)
    }
  }
}
