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

// Scala
import scala.collection.convert.decorateAsJava._
import scala.collection.convert.decorateAsScala._

// AWS DynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model._
import com.amazonaws.services.dynamodbv2.document.Table

// AWS S3
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ListObjectsV2Request

/**
  * Wrapper class for run manifests table
  * Can be used instead of functions from `RunManifests` module
  *
  * @param dynamodb AWS DynamoDB client
  * @param tableName existing DynamoDB table name with run manifests
  */
class RunManifests(dynamodb: AmazonDynamoDB, tableName: String) {
  /**
    * Creates DynamoDB table with all necessary settings
    * Should be called once, will throw exception otherwise
    */
  def create(): Unit =
    RunManifests.createManifestsTable(dynamodb, tableName)

  /**
    * Check whether run manifests table contains particular run id
    *
    * @param runId run id to check
    * @return true if run id is in table
    */
  def contains(runId: String): Boolean =
    RunManifests.isInManifests(dynamodb, tableName, runId)

  /**
    * Add run id to manifests table
    *
    * @param runId run id to add
    */
  def add(runId: String): Unit =
    RunManifests.addToManifests(dynamodb, tableName, runId)
}


object RunManifests {

  /**
    * Standard attribute name containing run id
    */
  val DynamoDbRunIdAttribute = "RunId"

  /**
    * List all run ids in specified S3 path (such as enriched-archive)
    *
    * @param s3 AWS S3 client
    * @param fullPath full S3 path (including bucket and prefix) to folder with run ids
    * @return list of prefixes (without S3 bucket)
    *         such as `storage/enriched/good/run=2017-02-21-11-40-15`
    */
  def listRunIds(s3: AmazonS3, fullPath: String): List[String] = {
    val (bucket, prefix) = splitFullPath(fullPath)
    val request = new ListObjectsV2Request()
      .withBucketName(bucket)
      .withDelimiter("/")
    prefix.foreach(request.withPrefix)
    s3.listObjectsV2(request).getCommonPrefixes.asScala.toList
  }

  /**
    * Creates DynamoDB table with all necessary settings
    * Should be called once, will throw exception otherwise
    *
    * @param dynamodb AWS DynamoDB client
    * @param tableName existing DynamoDB table name with run manifests
    */
  def createManifestsTable(dynamodb: AmazonDynamoDB, tableName: String): Unit = {
    val runIdAttribute = new AttributeDefinition(RunManifests.DynamoDbRunIdAttribute, ScalarAttributeType.S)
    val manifestsSchema = new KeySchemaElement(RunManifests.DynamoDbRunIdAttribute, KeyType.HASH)
    val manifestsThroughput = new ProvisionedThroughput(5L, 5L)
    val req = new CreateTableRequest()
      .withTableName(tableName)
      .withAttributeDefinitions(runIdAttribute)
      .withKeySchema(manifestsSchema)
      .withProvisionedThroughput(manifestsThroughput)

    dynamodb.createTable(req)
    RunManifests.waitForActive(dynamodb, tableName)
  }

  /**
    * Check whether run manifests table contains particular run id
    *
    * @param dynamodb AWS DynamoDB client
    * @param tableName existing DynamoDB table name with run manifests
    * @param runId run id to check
    * @return true if run id is in table
    */
  def isInManifests(dynamodb: AmazonDynamoDB, tableName: String, runId: String): Boolean = {
    val request = new GetItemRequest()
      .withTableName(tableName)
      .withKey(Map(RunManifests.DynamoDbRunIdAttribute -> new AttributeValue(runId)).asJava)
      .withAttributesToGet(RunManifests.DynamoDbRunIdAttribute)
    dynamodb.getItem(request).getItem.asScala.get(RunManifests.DynamoDbRunIdAttribute).isDefined
  }

  /**
    * Add run id to manifests table
    *
    * @param dynamodb AWS DynamoDB client
    * @param tableName existing DynamoDB table name with run manifests
    * @param runId run id to add
    */
  def addToManifests(dynamodb: AmazonDynamoDB, tableName: String, runId: String): Unit = {
    val request = new PutItemRequest()
      .withTableName(tableName)
      .withItem(Map(RunManifests.DynamoDbRunIdAttribute -> new AttributeValue(runId)).asJava)
    dynamodb.putItem(request)
  }

  /**
    * Wait until table is in `ACTIVE` state
    *
    * @param client AWS DynamoDB client
    * @param name DynamoDB table name
    */
  private def waitForActive(client: AmazonDynamoDB, name: String): Unit =
    new Table(client, name).waitForActive()

  /**
    * Split full S3 path with bucket and
    *
    * @param fullS3Path full S3 path with protocol, bucket name and optionally prefix
    * @return pair of bucket name (without "s3://) and prefix if it was specified
    *         (otherwise we're looking at root of bucket)
    */
  private[scalasdk] def splitFullPath(fullS3Path: String): (String, Option[String]) = {
    val parts = fullS3Path.stripPrefix("s3://").split("/").toList
    parts match {
      case bucket :: Nil => (bucket, None)
      case bucket :: prefix => (bucket, Some(prefix.mkString("/") + "/"))
    }
  }
}
