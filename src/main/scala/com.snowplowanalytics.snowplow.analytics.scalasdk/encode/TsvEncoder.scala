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

import magnolia._

import scala.language.experimental.macros

import com.snowplowanalytics.snowplow.analytics.scalasdk.SnowplowEvent._
import com.snowplowanalytics.snowplow.analytics.scalasdk.Event


import com.snowplowanalytics.snowplow.analytics.scalasdk.SnowplowEvent.Contexts
import com.snowplowanalytics.snowplow.analytics.scalasdk.SnowplowEvent.UnstructEvent

import Show.ops._

object TsvEncoder {

  def encode(event: Event): String =
    Show.ops.show(event)
}

trait Show[A] {
  def show(a: A): String
}

object Show {

  def apply[A](implicit sh: Show[A]): Show[A] = sh

  object ops {
    def show[A: Show](a: A) = Show[A].show(a)

    implicit class ShowOps[A: Show](a: A) {
      def show = Show[A].show(a)
    }
  }

  implicit val intCanShow: Show[Int] =
    i => i.toString

  implicit val stringCanShow: Show[String] =
    s => s

  implicit val doubleCanShow: Show[Double] =
    d => d.toString

  implicit val booleanCanShow: Show[Boolean] =
    b => if (b) "1" else "0"
  
  implicit val uuidCanShow: Show[UUID] =
    u => u.toString
  
    implicit val optUuidCanShow: Show[Option[UUID]] =
    optU => optU match {
      case Some(u) => u.show
      case None => ""
    } 

  implicit val instantCanShow: Show[Instant] =
    inst => 
      DateTimeFormatter.ISO_INSTANT
        .format(inst)
        .replace("T", " ")
        .dropRight(1) // remove trailing 'Z'

   implicit val contextsCanShow: Show[Contexts] =
     ctxts => 
       if (ctxts.data.isEmpty)
         ""
       else
         ctxts.asJson.noSpaces

   implicit val unstructCanShow: Show[UnstructEvent] =
     unstruct =>
       if (unstruct.data.isDefined)
         unstruct.asJson.noSpaces
       else
         ""
  
  type Typeclass[T] = Show[T] // required otherwise does not compile

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] =
    new Show[T] {
      def show(a: T): String = {
        caseClass.parameters.map { p =>
          s"${p.typeclass.show(p.dereference(a))}"
        }.mkString("\t")
      }
    }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] =
    new Show[T] {
      def show(a: T): String = {
        sealedTrait.dispatch(a) { subtype =>
          subtype.typeclass.show(subtype.cast(a))
        }
      }
    }

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]
}
