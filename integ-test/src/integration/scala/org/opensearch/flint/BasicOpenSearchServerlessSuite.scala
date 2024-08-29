/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.flint

import com.stephenn.scalatest.jsonassert.JsonMatchers.matchJson
import org.apache.spark.{FlintSuite, SparkConf}
import org.apache.spark.sql.flint.config.FlintSparkConf.{AUTH, CHECKPOINT_MANDATORY, HOST_ENDPOINT, HOST_PORT, REFRESH_POLICY, REGION, SCHEME, SERVICE_NAME}
import org.apache.spark.sql.{ExplainSuiteHelper, QueryTest}
import org.apache.spark.sql.streaming.StreamTest
import org.opensearch.flint.common.FlintVersion.current
import org.opensearch.flint.core.storage.FlintOpenSearchIndexMetadataService
import org.opensearch.flint.spark.FlintSpark
import org.opensearch.flint.spark.covering.FlintSparkCoveringIndex.getFlintIndexName

class BasicOpenSearchServerlessSuite
  extends QueryTest
  with StreamTest
  with FlintSuite
  with OpenSearchServerlessSuite
  with ExplainSuiteHelper {

  lazy protected val flint: FlintSpark = new FlintSpark(spark)
  lazy protected val tableType: String = "CSV"
  lazy protected val tableOptions: String = "OPTIONS (header 'false', delimiter '\t')"
  private val testTable = "spark_catalog.default.ci_test"
  private val testIndex = "name_and_age"
  private val testFlintIndex = getFlintIndexName(testIndex, testTable)

  override protected def sparkConf: SparkConf = {
    val conf = super.sparkConf
      .set(HOST_ENDPOINT.key, openSearchHost)
      .set(HOST_PORT.key, openSearchPort)
      .set(SCHEME.key, openSearchScheme)
      .set(REFRESH_POLICY.key, "false")
      // Disable mandatory checkpoint for test convenience
      .set(CHECKPOINT_MANDATORY.key, "false")
      .set(AUTH.key, "sigv4")
      .set(SERVICE_NAME.key, "aoss")
      .set(REGION.key, "us-west-2")
    conf
  }

  test("create covering index with metadata successfully") {
    createPartitionedAddressTable(testTable)
    flint
      .coveringIndex()
      .name(testIndex)
      .onTable(testTable)
      .addIndexColumns("name", "age")
      .filterBy("age > 30")
      .create()

    val index = flint.describeIndex(testFlintIndex)
  }

  protected def createPartitionedAddressTable(testTable: String): Unit = {
    sql(s"""
           | CREATE TABLE $testTable
           | (
           |   name STRING,
           |   age INT,
           |   address STRING
           | )
           | USING $tableType $tableOptions
           | PARTITIONED BY (
           |    year INT,
           |    month INT
           | )
           |""".stripMargin)

    sql(s"""
           | INSERT INTO $testTable
           | PARTITION (year=2023, month=4)
           | VALUES ('Hello', 30, 'Seattle')
           | """.stripMargin)

    sql(s"""
           | INSERT INTO $testTable
           | PARTITION (year=2023, month=5)
           | VALUES ('World', 25, 'Portland')
           | """.stripMargin)
  }


}
