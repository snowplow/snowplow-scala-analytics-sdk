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

// Scala
import scala.collection.convert.decorateAsJava._
import scala.collection.convert.decorateAsScala._

// AWS DynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model._
import com.amazonaws.services.dynamodbv2.document.Table

// AWS S3
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ ListObjectsV2Request, ListObjectsV2Result }

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


/**
 * Module with primary run-manifests functions, without applied client
 */
object RunManifests {

  /**
   * S3 protocol implementations that can be used for `listRunIds` `fullPath` argument
   */
  val SupportedPrefixes = Set("s3://", "s3n://", "s3a://")

  /**
    * Standard attribute name containing run id
    */
  val DynamoDbRunIdAttribute = "RunId"

  /**
   * `RunManifests` short-hand constructor
   */
  def apply(dynamodb: AmazonDynamoDB, tableName: String): RunManifests =
    new RunManifests(dynamodb, tableName)

  /**
    * List *all* (regardless `MaxKeys`) run ids in specified S3 path
   * (such as enriched-archive)
    *
    * @param s3 AWS S3 client
    * @param fullPath full S3 path (including bucket and prefix) to folder with run ids
    * @return list of prefixes (without S3 bucket)
    *         such as `storage/enriched/good/run=2017-02-21-11-40-15`
    */
  def listRunIds(s3: AmazonS3, fullPath: String): List[String] = {
    // Initialize mutable buffer
    val buffer = collection.mutable.ListBuffer.empty[String]
    var result: ListObjectsV2Result = null

    val (bucket, prefix) = splitFullPath(fullPath) match {
      case Right((b, p)) => (b, p)
      case Left(error) => throw new RuntimeException(error)
    }

    val glacierified: String => Boolean =
      isGlacierified(s3, bucket, _)

    val req = new ListObjectsV2Request()
      .withBucketName(bucket)
      .withDelimiter("/")
      .withPrefix(prefix.orNull)

    do {
      result = s3.listObjectsV2(req)
      val objects = result.getCommonPrefixes.asScala.toList.filterNot(glacierified)
      buffer ++= objects

      req.setContinuationToken(result.getNextContinuationToken)
    } while(result.isTruncated)

    buffer.toList
  }


  /**
   * Check if specified prefix (directory) contains objects archived to AWS Glacier
   *
   * @param s3 AWS S3 client
   * @param bucket AWS S3 bucket (without prefix and trailing slash)
   * @param prefix full prefix with trailing slash
   * @return true if any of first 3 objects has GLACIER storage class
   */
  private def isGlacierified(s3: AmazonS3, bucket: String, prefix: String): Boolean = {
    val req = new ListObjectsV2Request()
      .withBucketName(bucket)
      .withPrefix(prefix)
      .withMaxKeys(3)   // Use 3 to not accidentally fetch _SUCCESS or ghost files

    val classes = s3.listObjectsV2(req).getObjectSummaries.asScala.map(_.getStorageClass)
    classes.contains("GLACIER")
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

    try {
      dynamodb.createTable(req)
    } catch {
      case _: ResourceInUseException => ()
    }
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
    val key = Map(RunManifests.DynamoDbRunIdAttribute -> new AttributeValue(runId)).asJava
    val request = new GetItemRequest()
      .withTableName(tableName)
      .withKey(key)
      .withAttributesToGet(RunManifests.DynamoDbRunIdAttribute)
    Option(dynamodb.getItem(request).getItem).isDefined
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
  private[scalasdk] def splitFullPath(fullS3Path: String): Either[String, (String, Option[String])] = {
    stripPrefix(fullS3Path) match {
      case Right(path) =>
        val parts = path.split("/").toList
        parts match {
          case bucket :: Nil => Right((bucket, None))
          case bucket :: prefix => Right((bucket, Some(prefix.mkString("/") + "/")))
          case _ => Left("Cannot split path") // Cannot happen
        }
      case Left(error) => Left(error)
    }
  }

  /**
   * Remove one of allowed prefixed (s3://, s3n:// etc) from S3 path
   *
   * @param path full S3 path with prefix
   * @return right S3 path without prefix or error
   */
  private[scalasdk] def stripPrefix(path: String): Either[String, String] = {
    val error: Either[String, String] =
      Left(s"S3 path [$path] doesn't start with one of possible prefixes: [${SupportedPrefixes.mkString(", ")}]")

    SupportedPrefixes.foldLeft(error) { (result, prefix) =>
      result match {
        case Left(_) if path.startsWith(prefix) => Right(path.stripPrefix(prefix))
        case other => other
      }
    }
  }
}
