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
