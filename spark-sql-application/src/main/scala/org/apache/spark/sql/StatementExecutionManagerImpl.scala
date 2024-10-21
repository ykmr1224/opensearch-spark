/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.apache.spark.sql

import org.opensearch.flint.common.model.FlintStatement
import org.opensearch.flint.core.metrics.MetricsUtil
import org.opensearch.flint.core.storage.OpenSearchUpdater
import org.opensearch.search.sort.SortOrder

import org.apache.spark.internal.Logging
import org.apache.spark.scheduler.{SparkListener, SparkListenerJobEnd, SparkListenerJobStart, SparkListenerTaskEnd}
import org.apache.spark.sql.util.ReadWriteBytesSparkListener

/**
 * StatementExecutionManagerImpl is session based implementation of StatementExecutionManager
 * interface It uses FlintReader to fetch all pending queries in a mirco-batch
 * @param commandContext
 */
class StatementExecutionManagerImpl(commandContext: CommandContext)
    extends StatementExecutionManager
    with FlintJobExecutor
    with Logging {

  private val context = commandContext.sessionManager.getSessionContext
  private val sessionIndex = context("sessionIndex").asInstanceOf[String]
  private val resultIndex = context("resultIndex").asInstanceOf[String]
  private val osClient = context("osClient").asInstanceOf[OSClient]
  private val flintSessionIndexUpdater =
    context("flintSessionIndexUpdater").asInstanceOf[OpenSearchUpdater]

  // Using one reader client within same session will cause concurrency issue.
  // To resolve this move the reader creation and getNextStatement method to mirco-batch level
  private val flintReader = createOpenSearchQueryReader()

  override def prepareStatementExecution(): Either[String, Unit] = {
    checkAndCreateIndex(osClient, resultIndex)
  }
  override def updateStatement(statement: FlintStatement): Unit = {
    flintSessionIndexUpdater.update(statement.statementId, FlintStatement.serialize(statement))
  }
  override def terminateStatementExecution(): Unit = {
    flintReader.close()
  }

  override def getNextStatement(): Option[FlintStatement] = {
    if (flintReader.hasNext) {
      val rawStatement = flintReader.next()
      val flintStatement = FlintStatement.deserialize(rawStatement)
      logInfo(s"Next statement to execute: $flintStatement")
      Some(flintStatement)
    } else {
      None
    }
  }

  override def executeStatement(statement: FlintStatement): DataFrame = {
    import commandContext.spark
    // we have to set job group in the same thread that started the query according to spark doc
    spark.sparkContext.setJobGroup(
      statement.queryId,
      "Job group for " + statement.queryId,
      interruptOnCancel = true)
    spark.sparkContext.addSparkListener(new ReadWriteBytesSparkListener(statement.queryId))
    spark.sql(statement.query)
  }

  private def createOpenSearchQueryReader() = {
    import commandContext._
    // all state in index are in lower case
    // we only search for statement submitted in the last hour in case of unexpected bugs causing infinite loop in the
    // same doc
    val dsl =
      s"""{
       |  "bool": {
       |    "must": [
       |    {
       |        "term": {
       |          "type": "statement"
       |        }
       |      },
       |      {
       |        "term": {
       |          "state": "waiting"
       |        }
       |      },
       |      {
       |        "term": {
       |          "sessionId": "$sessionId"
       |        }
       |      },
       |      {
       |        "term": {
       |          "dataSourceName": "$dataSource"
       |        }
       |      },
       |      {
       |        "range": {
       |          "submitTime": { "gte": "now-1h" }
       |        }
       |      }
       |    ]
       |  }
       |}""".stripMargin
    val flintReader = osClient.createQueryReader(sessionIndex, dsl, "submitTime", SortOrder.ASC)
    flintReader
  }
}
