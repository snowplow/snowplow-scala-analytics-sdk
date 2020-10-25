/*
 * Copyright (c) 2016-2019 Snowplow Analytics Ltd. All rights reserved.
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

import com.snowplowanalytics.iglu.core.SchemaKey

/**
 * Common data types for enriched event
 */
object Data {

  /**
   * The type (contexts/derived_contexts/unstruct_event) and Iglu URI of a shredded type
   */
  case class ShreddedType(shredProperty: ShredProperty, schemaKey: SchemaKey)

  /**
   * Known contexts types of enriched event
   */
  sealed trait ContextsType {
    def field: String
  }

  case object DerivedContexts extends ContextsType {
    def field = "derived_contexts"
  }

  case object CustomContexts extends ContextsType {
    def field = "contexts"
  }

  /**
   * Field types of enriched event that can be shredded (self-describing JSONs)
   */
  sealed trait ShredProperty {

    /**
     * Canonical field name
     */
    def name: String

    /**
     * Result output prefix
     */
    def prefix: String
  }

  case class Contexts(contextType: ContextsType) extends ShredProperty {
    def name = contextType.field
    def prefix = "contexts_"
  }

  case object UnstructEvent extends ShredProperty {
    def name = "unstruct_event"
    def prefix = name + "_"
  }
}
