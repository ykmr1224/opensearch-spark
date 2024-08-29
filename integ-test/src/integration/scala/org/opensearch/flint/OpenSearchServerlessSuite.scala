/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.flint

import java.util
import org.apache.http.HttpHost
import org.apache.spark.sql.flint.config.FlintSparkConf
import org.apache.spark.sql.flint.config.FlintSparkConf.{HOST_ENDPOINT, HOST_PORT, IGNORE_DOC_ID_COLUMN, REFRESH_POLICY}
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest
import org.opensearch.action.bulk.BulkRequest
import org.opensearch.action.index.IndexRequest
import org.opensearch.action.support.WriteRequest.RefreshPolicy
import org.opensearch.client.indices.{CreateIndexRequest, GetIndexRequest}
import org.opensearch.client.{RequestOptions, RestClient, RestHighLevelClient}
import org.opensearch.common.xcontent.XContentType
import org.opensearch.flint.core.FlintOptions
import org.opensearch.flint.core.storage.OpenSearchClientUtils
import org.opensearch.testcontainers.OpenSearchContainer
import org.scalatest.{BeforeAndAfterAll, Suite}

import scala.jdk.CollectionConverters.mapAsJavaMapConverter

/**
 * Test required OpenSearch domain should extend OpenSearchSuite.
 */
trait OpenSearchServerlessSuite extends BeforeAndAfterAll {
  self: Suite =>

  protected lazy val openSearchPort = "443"
  protected lazy val openSearchHost = "8lcitp965p4i2ed4r8zj.us-west-2.aoss.amazonaws.com"
  protected lazy val openSearchScheme = "https"

  protected lazy val openSearchOptions =
    Map(
      FlintOptions.AUTH-> FlintOptions.SIGV4_AUTH,
      FlintOptions.SERVICE_NAME-> FlintOptions.SERVICE_NAME_AOSS,
      FlintOptions.REGION-> "us-west-2",
      FlintOptions.HOST-> openSearchHost,
      FlintOptions.PORT-> openSearchPort,
      FlintOptions.SCHEME-> openSearchScheme
    )

  protected lazy val openSearchClient = OpenSearchClientUtils.createRestHighLevelClient(
    new FlintOptions(openSearchOptions.asJava))

  //  protected lazy val openSearchOptions =
  //    Map(
  //      s"${HOST_ENDPOINT.optionKey}" -> openSearchHost,
  //      s"${HOST_PORT.optionKey}" -> s"$openSearchPort",
  //      s"${REFRESH_POLICY.optionKey}" -> "none",
  //      s"${IGNORE_DOC_ID_COLUMN.optionKey}" -> "false")

  override def beforeAll(): Unit = {
    metadataIndex()
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  /**
   * Delete index `indexNames` after calling `f`.
   */
  protected def withIndexName(indexNames: String*)(f: => Unit): Unit = {
    try {
      f
    } finally {
      indexNames.foreach { indexName =>
        openSearchClient
          .indices()
          .delete(new DeleteIndexRequest(indexName), RequestOptions.DEFAULT)
      }
    }
  }

  def metadataIndex(): Unit = {
    if (!openSearchClient.indices.exists(new GetIndexRequest("query_execution_request"), RequestOptions.DEFAULT)) {
      openSearchClient.indices.create(
        new CreateIndexRequest("query_execution_request"),
        RequestOptions.DEFAULT)
    }
  }


    val oneNodeSetting = """{
                         |  "number_of_shards": "1",
                         |  "number_of_replicas": "0"
                         |}""".stripMargin

  val multipleShardSetting = """{
                         |  "number_of_shards": "2",
                         |  "number_of_replicas": "0"
                         |}""".stripMargin

  def simpleIndex(indexName: String): Unit = {
    val mappings = """{
                     |  "properties": {
                     |    "accountId": {
                     |      "type": "keyword"
                     |    },
                     |    "eventName": {
                     |      "type": "keyword"
                     |    },
                     |    "eventSource": {
                     |      "type": "keyword"
                     |    }
                     |  }
                     |}""".stripMargin
    val docs = Seq("""{
                     |  "accountId": "123",
                     |  "eventName": "event",
                     |  "eventSource": "source"
                     |}""".stripMargin)
    index(indexName, oneNodeSetting, mappings, docs)
  }

  def multipleDocIndex(indexName: String, N: Int): Unit = {
    val mappings = """{
                     |  "properties": {
                     |    "id": {
                     |      "type": "integer"
                     |    }
                     |  }
                     |}""".stripMargin

    val docs = for (n <- 1 to N) yield s"""{"id": $n}""".stripMargin
    index(indexName, multipleShardSetting, mappings, docs)
  }

  def multipleShardAndDocIndex(indexName: String, N: Int): Unit = {
    val mappings = """{
                     |  "properties": {
                     |    "id": {
                     |      "type": "integer"
                     |    }
                     |  }
                     |}""".stripMargin

    val docs = for (n <- 1 to N) yield s"""{"id": $n}""".stripMargin
    index(indexName, oneNodeSetting, mappings, docs)
  }

  def index(index: String, settings: String, mappings: String, docs: Seq[String]): Unit = {
    openSearchClient.indices.create(
      new CreateIndexRequest(index)
        .settings(settings, XContentType.JSON)
        .mapping(mappings, XContentType.JSON),
      RequestOptions.DEFAULT)

    val getIndexResponse =
      openSearchClient.indices().get(new GetIndexRequest(index), RequestOptions.DEFAULT)
    assume(getIndexResponse.getIndices.contains(index), s"create index $index failed")

    /**
     *   1. Wait until refresh the index.
     */
    if (docs.nonEmpty) {
      val request = new BulkRequest().setRefreshPolicy(RefreshPolicy.WAIT_UNTIL)
      for (doc <- docs) {
        request.add(new IndexRequest(index).source(doc, XContentType.JSON))
      }

      val response =
        openSearchClient.bulk(request, RequestOptions.DEFAULT)

      assume(
        !response.hasFailures,
        s"bulk index docs to $index failed: ${response.buildFailureMessage()}")
    }
  }
}
