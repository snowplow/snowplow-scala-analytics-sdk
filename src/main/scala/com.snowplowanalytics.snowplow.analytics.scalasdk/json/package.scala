 /*
 * Copyright (c) 2016-2018 Snowplow Analytics Ltd.
 * All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache
 * License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 *
 * See the Apache License Version 2.0 for the specific language
 * governing permissions and limitations there under.
 */
package com.snowplowanalytics.snowplow.analytics.scalasdk

// This library
import json.Data._


package object json {

  type Validated[+A] = Either[List[String], A]

  /**
   * The event as a shredded, stringified JSON on Success,
   * or a strings on Failure.
   */
  type ValidatedEvent = Validated[String]

  /**
   * The event as a shredded, stringified JSON with inventory on Success,
   * or a strings on Failure.
   */
  type ValidatedEventWithInventory = Validated[EventWithInventory]

  /**
   * Functions used to change a TSV pair to a JObject
   * Usually returns single key-value pair, but for JSON fields, such as
   * contexts returns all contained key-value pairs
   */
  type TsvToJsonConverter = (String, String) => Validated[TsvConverterOutput]


  /**
   * Replacement for Scalaz Either type classes
   */
  private[scalasdk] implicit class EitherSyntax[L, R](either: Either[L, R]) {
    /**
     * Short-circuiting semantics for Either (Monad)
     */
    def flatMap[RR](f: R => Either[L, RR]) = either match {
      case Right(r) => f(r)
      case Left(l) => Left(l)
    }

    /**
     * Functor for Either
     */
    def map[RR](f: R => RR) = either match {
      case Right(r) => Right(f(r))
      case Left(l) => Left(l)
    }

    /**
     * Applicative for Either
     */
    def map2[RO, RR](other: Either[L, RO])(f: (R, RO) => RR): Either[List[L], RR] = either match {
      case Right(r) => other match {
        case Right(ro) => Right(f(r, ro))
        case Left(l) => Left(List(l))
      }
      case Left(l) => other match {
        case Right(_) => Left(List(l))
        case Left(lo) => Left(List(l, lo))
      }
    }
  }

  /**
   * Replacement for Scalaz Traverse type-class syntax,
   * defined specifically for `List[Either[L, R]]`
   */
  private[scalasdk] implicit class ListSyntax[+A](list: List[A]) {
    /**
     * Scalaz .sequence replacement for short-circuiting list of `Either`s
     */
    def traverseEither[L, R](implicit ev: A <:< Either[L, R]): Either[L, List[R]] =
      list.foldLeft(Right(Nil): Either[L, List[R]]) { (acc, cur) => cur match {
        case Right(r: R @unchecked) => acc.map(r :: _)
        case Left(l: L @unchecked) => Left(l)
      } }


    private type EitherAccum[L, R] = (Either[List[L], List[R]], Either[List[L], List[R]])

    /**
     * Scalaz .sequence replacement for error-accumulating list of `Either`s
     */
    def traverseEitherL[L, R](implicit ev: A <:< Either[List[L], R]): Either[List[L], List[R]] = {
      val accum = list.foldLeft(empty[L, R]) { (acc: EitherAccum[L, R], cur: A) => (acc, cur) match {
        case ((Left(Nil),   rights), Right(r: R @unchecked)) => (Left[List[L], List[R]](Nil), rights.map(r :: _))
        case ((Left(lefts), _),      Left(l: List[L @unchecked])) => (Left(l ++ lefts), Right(Nil))
        case ((Left(lefts), _),      Right(_)) => (Left(lefts), Right(Nil))
        case a                                 => throw new RuntimeException(s"Invalid state in traverseEitherL\n\n$a")
      } }
      
      reverseAccum(accum) match {
        case (Left(Nil), rights) => rights
        case (lefts, Right(Nil)) => lefts
        case _ => throw new RuntimeException("Invalid state in traverseEitherL")
        
      }
    }

    private def reverseAccum[L, R](eithers: EitherAccum[L, R]): EitherAccum[L, R] = eithers match {
      case (Left(lefts), Right(rights)) => (Left(lefts.reverse), Right(rights.reverse))
      case _ => throw new RuntimeException("Invalid state in traverseEitherL 3")

    }

    private def empty[L, R]: EitherAccum[L, R] = (Left(Nil), Right(Nil))
  }

}
